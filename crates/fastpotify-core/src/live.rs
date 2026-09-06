//! Headless Spotify application state for native frontends.
//!
//! This is deliberately UI-free. Android and any future frontend drive the
//! same authenticated Web API client used by the desktop application and
//! consume a small serializable snapshot.

use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};

use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};

use crate::api::ApiSource;
use crate::api::client::{ApiClient, NetActivity, PlayRequest, TokenProvider, WebTokens};
use crate::api::models::{
    Device, Image, PlayableItem, PlaybackState, Playlist, SearchResults, Track, User, pick_image,
};
use crate::auth::{self, Grant, StoredToken};
use crate::player::{
    Engine, EngineConfig, EngineEvent, LoadSpec, LocalState, Playback, PlayerCommand, RepeatMode,
};
use librespot_core::{authentication::Credentials, cache::Cache};

const TOKEN_FILE: &str = "spotify-web-token.json";
const SETTINGS_FILE: &str = "android-settings.json";
const IMAGE_TARGET: u32 = 360;

#[derive(Clone, Debug, Default, Deserialize, Serialize, PartialEq)]
pub struct LiveSnapshot {
    pub contract_version: u32,
    pub revision: u64,
    pub auth_status: AuthState,
    pub busy: bool,
    pub error: Option<String>,
    pub user: Option<LiveUser>,
    pub playlists: Vec<LiveCard>,
    pub saved_tracks: Vec<LiveTrack>,
    pub library_items: Vec<LiveCard>,
    pub made_for_you: Vec<LiveCard>,
    pub top_artists: Vec<LiveCard>,
    pub top_tracks: Vec<LiveTrack>,
    pub recommendations: Vec<LiveTrack>,
    pub recent_tracks: Vec<LiveTrack>,
    pub search_query: String,
    pub search_results: Vec<LiveCard>,
    pub opened_playlist: Option<LivePlaylist>,
    pub now_playing: Option<LiveNowPlaying>,
    pub queue: Vec<LiveTrack>,
    pub devices: Vec<LiveDevice>,
    pub local_playback: LocalPlaybackState,
    pub local_error: Option<String>,
    pub settings: MobileSettings,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, Serialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum AuthState {
    #[default]
    SignedOut,
    SigningIn,
    SignedIn,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, Serialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum LocalPlaybackState {
    #[default]
    SignedOut,
    SigningIn,
    Connecting,
    Connected,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq, Eq)]
pub struct MobileSettings {
    pub bitrate_kbps: u16,
    pub normalisation: bool,
    pub autoplay: bool,
    pub gapless: bool,
    pub cache_mb: u32,
}

impl Default for MobileSettings {
    fn default() -> Self {
        Self {
            bitrate_kbps: 320,
            normalisation: true,
            autoplay: true,
            gapless: true,
            cache_mb: 1024,
        }
    }
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LiveUser {
    pub id: String,
    pub name: String,
    pub image_url: Option<String>,
    pub product: Option<String>,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LiveCard {
    pub id: String,
    pub uri: String,
    pub title: String,
    pub subtitle: String,
    pub image_url: Option<String>,
    pub kind: String,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LiveTrack {
    pub id: String,
    pub uri: String,
    pub title: String,
    pub artist: String,
    pub album: String,
    pub image_url: Option<String>,
    pub duration_ms: u32,
    pub explicit: bool,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LivePlaylist {
    pub id: String,
    pub uri: String,
    pub title: String,
    pub description: String,
    pub owner: String,
    pub image_url: Option<String>,
    pub total: u32,
    pub tracks: Vec<LiveTrack>,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LiveNowPlaying {
    pub track: LiveTrack,
    pub position_ms: u32,
    pub playing: bool,
    pub shuffled: bool,
    pub repeat: String,
    pub device_id: Option<String>,
}

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct LiveDevice {
    pub id: Option<String>,
    pub name: String,
    pub kind: String,
    pub active: bool,
    pub restricted: bool,
    pub volume_percent: Option<u8>,
}

struct State {
    snapshot: LiveSnapshot,
    api: Option<Arc<ApiClient>>,
    engine: Option<Arc<Engine>>,
}

/// Owns the async runtime and the headless Spotify state.
pub struct MobileClient {
    runtime: tokio::runtime::Runtime,
    http: reqwest::Client,
    state: Arc<Mutex<State>>,
    token_path: PathBuf,
    files_dir: PathBuf,
}

impl MobileClient {
    pub fn new(files_dir: impl AsRef<Path>) -> Result<Self> {
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .thread_name("fastpotify-mobile")
            .build()
            .context("unable to start the Fastpotify runtime")?;
        let http = reqwest::Client::builder()
            .user_agent(concat!("Fastpotify-Android/", env!("CARGO_PKG_VERSION")))
            .build()
            .context("unable to create the Spotify HTTP client")?;
        let files_dir = files_dir.as_ref().to_path_buf();
        std::fs::create_dir_all(&files_dir).context("unable to create the app data directory")?;
        let token_path = files_dir.join(TOKEN_FILE);
        let settings = load_settings(&files_dir);
        let token =
            StoredToken::load(&token_path).filter(|token| token.has_scopes(auth::WEB_SCOPES));
        let api = token.map(|token| api_client(&http, token, &token_path));
        let auth_status = if api.is_some() {
            AuthState::SignedIn
        } else {
            AuthState::SignedOut
        };
        let client = Self {
            runtime,
            http,
            state: Arc::new(Mutex::new(State {
                snapshot: LiveSnapshot {
                    contract_version: crate::CONTRACT_VERSION,
                    auth_status,
                    settings,
                    ..LiveSnapshot::default()
                },
                api,
                engine: None,
            })),
            token_path,
            files_dir,
        };
        if auth_status == AuthState::SignedIn {
            client.refresh();
        }
        client.restore_local_playback();
        Ok(client)
    }

    pub fn snapshot_json(&self) -> String {
        let (mut snapshot, engine) = {
            let state = lock(&self.state);
            (state.snapshot.clone(), state.engine.clone())
        };
        if let Some(engine) = engine {
            apply_local_state(&mut snapshot, &engine.state_snapshot());
        }
        serde_json::to_string(&snapshot).unwrap_or_else(|error| {
            format!(
                r#"{{"contract_version":{},"error":"{error}"}}"#,
                crate::CONTRACT_VERSION
            )
        })
    }

    /// Starts the existing Fastpotify PKCE flow and returns the browser URL.
    /// The loopback listener completes in the background after Spotify sends
    /// the browser back to this device.
    pub fn start_sign_in(&self) -> Result<String> {
        let grant = Grant::shared_web_api();
        let flow = auth::begin(grant.clone());
        {
            let mut state = lock(&self.state);
            state.snapshot.auth_status = AuthState::SigningIn;
            state.snapshot.busy = true;
            state.snapshot.error = None;
            bump(&mut state.snapshot);
        }

        let url = flow.url.clone();
        let state = Arc::clone(&self.state);
        let http = self.http.clone();
        let token_path = self.token_path.clone();
        self.runtime.spawn(async move {
            let result = async {
                let (_cancel_tx, cancel_rx) = tokio::sync::watch::channel(false);
                let code = auth::wait_for_code(grant.redirect_port, &flow.state, cancel_rx).await?;
                let response = auth::exchange_code(&http, &grant, &code, &flow.verifier).await?;
                let token = StoredToken::from_response(&grant.client_id, response, None)?;
                token.save(&token_path)?;
                let api = api_client(&http, token, &token_path);
                let dashboard = fetch_dashboard(&api).await.map_err(anyhow::Error::msg)?;
                Ok::<_, anyhow::Error>((api, dashboard))
            }
            .await;

            match result {
                Ok((api, dashboard)) => {
                    let mut guard = lock(&state);
                    guard.api = Some(api);
                    guard.snapshot.auth_status = AuthState::SignedIn;
                    apply_dashboard(&mut guard.snapshot, dashboard);
                }
                Err(error) => {
                    let mut guard = lock(&state);
                    guard.snapshot.auth_status = AuthState::SignedOut;
                    guard.snapshot.busy = false;
                    guard.snapshot.error = Some(format!("Не удалось войти в Spotify: {error}"));
                    bump(&mut guard.snapshot);
                }
            }
        });
        Ok(url)
    }

    pub fn sign_out(&self) {
        StoredToken::remove(&self.token_path);
        let engine = {
            let mut state = lock(&self.state);
            state.api = None;
            let engine = state.engine.take();
            let settings = state.snapshot.settings.clone();
            state.snapshot = LiveSnapshot {
                contract_version: crate::CONTRACT_VERSION,
                revision: state.snapshot.revision.saturating_add(1),
                settings,
                ..LiveSnapshot::default()
            };
            engine
        };
        if let Some(engine) = engine {
            engine.shutdown();
        }
        let _ = std::fs::remove_dir_all(self.files_dir.join("librespot").join("credentials"));
    }

    /// Starts the independent librespot grant used for local audio and
    /// Spotify Connect. Spotify requires this approval separately from the
    /// Web API grant.
    pub fn start_local_sign_in(&self) -> Result<String> {
        let grant = Grant::playback();
        let flow = auth::begin(grant.clone());
        {
            let mut state = lock(&self.state);
            state.snapshot.local_playback = LocalPlaybackState::SigningIn;
            state.snapshot.local_error = None;
            bump(&mut state.snapshot);
        }
        let url = flow.url.clone();
        let state = Arc::clone(&self.state);
        let http = self.http.clone();
        let config = playback_config(&self.files_dir, &lock(&self.state).snapshot.settings);
        self.runtime.spawn(async move {
            let result = async {
                let (_cancel_tx, cancel_rx) = tokio::sync::watch::channel(false);
                let code = auth::wait_for_code(grant.redirect_port, &flow.state, cancel_rx).await?;
                let token = auth::exchange_code(&http, &grant, &code, &flow.verifier).await?;
                let cache = config.open_cache()?;
                let credentials = Credentials::with_access_token(token.access_token);
                connect_engine(config, credentials, cache, Arc::clone(&state)).await
            }
            .await;
            if let Err(error) = result {
                let mut guard = lock(&state);
                guard.snapshot.local_playback = LocalPlaybackState::SignedOut;
                guard.snapshot.local_error = Some(format!("Локальное воспроизведение: {error}"));
                bump(&mut guard.snapshot);
            }
        });
        Ok(url)
    }

    fn restore_local_playback(&self) {
        let config = playback_config(&self.files_dir, &lock(&self.state).snapshot.settings);
        let Ok(cache) = config.open_cache() else {
            return;
        };
        let Some(credentials) = cache.credentials() else {
            return;
        };
        {
            let mut state = lock(&self.state);
            state.snapshot.local_playback = LocalPlaybackState::Connecting;
            bump(&mut state.snapshot);
        }
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            if let Err(error) = connect_engine(config, credentials, cache, Arc::clone(&state)).await
            {
                let mut guard = lock(&state);
                guard.snapshot.local_playback = LocalPlaybackState::SignedOut;
                guard.snapshot.local_error = Some(format!("Spotify Connect: {error}"));
                bump(&mut guard.snapshot);
            }
        });
    }

    pub fn refresh(&self) {
        let Some(api) = self.api() else {
            return;
        };
        self.set_busy();
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            match fetch_dashboard(&api).await {
                Ok(data) => {
                    let mut guard = lock(&state);
                    guard.snapshot.auth_status = AuthState::SignedIn;
                    apply_dashboard(&mut guard.snapshot, data);
                }
                Err(error) => finish_error(&state, error),
            }
        });
    }

    pub fn search(&self, query: String) {
        let Some(api) = self.api() else {
            return;
        };
        {
            let mut state = lock(&self.state);
            state.snapshot.search_query = query.clone();
            state.snapshot.busy = true;
            state.snapshot.error = None;
            bump(&mut state.snapshot);
        }
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            let result = api
                .search(
                    &query,
                    &["track", "artist", "album", "playlist", "show", "episode"],
                )
                .await;
            match result {
                Ok(results) => {
                    let mut guard = lock(&state);
                    // Do not let an older request replace results for newer text.
                    if guard.snapshot.search_query == query {
                        guard.snapshot.search_results = map_search(results);
                        guard.snapshot.busy = false;
                        guard.snapshot.error = None;
                        bump(&mut guard.snapshot);
                    }
                }
                Err(error) => finish_error(&state, format!("Поиск не выполнен: {error}")),
            }
        });
    }

    pub fn open_playlist(&self, id: String) {
        self.open_content("playlist".into(), id);
    }

    pub fn open_content(&self, kind: String, id: String) {
        let Some(api) = self.api() else {
            return;
        };
        self.set_busy();
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            let result = async {
                match kind.as_str() {
                    "playlist" => {
                        let (playlist, items) =
                            tokio::try_join!(api.playlist(&id), api.playlist_items(&id, 0, 100),)
                                .map_err(|error| error.to_string())?;
                        Ok(LivePlaylist {
                            id: playlist.id.clone(),
                            uri: playlist.uri.clone(),
                            title: playlist.name.clone(),
                            description: playlist.description.clone().unwrap_or_default(),
                            owner: playlist.owner_name().to_string(),
                            image_url: image_url(&playlist.images),
                            total: items.total,
                            tracks: items
                                .items
                                .iter()
                                .filter_map(|item| item.playable())
                                .map(map_playable)
                                .collect(),
                        })
                    }
                    "album" => {
                        let (album, tracks) =
                            tokio::try_join!(api.album(&id), api.album_tracks(&id, 0, 50))
                                .map_err(|error| error.to_string())?;
                        Ok(LivePlaylist {
                            id: album.id.clone(),
                            uri: album.uri.clone(),
                            title: album.name.clone(),
                            description: album.label.clone().unwrap_or_default(),
                            owner: album
                                .artists
                                .iter()
                                .map(|artist| artist.name.as_str())
                                .collect::<Vec<_>>()
                                .join(", "),
                            image_url: image_url(&album.images),
                            total: tracks.total,
                            tracks: tracks
                                .items
                                .iter()
                                .map(|track| map_album_track(track, &album))
                                .collect(),
                        })
                    }
                    "artist" => {
                        let (artist, tracks) =
                            tokio::try_join!(api.artist(&id), api.artist_top_tracks(&id))
                                .map_err(|error| error.to_string())?;
                        Ok(LivePlaylist {
                            id: artist.id.clone(),
                            uri: artist.uri.clone(),
                            title: artist.name.clone(),
                            description: artist.genres.join(" · "),
                            owner: "Исполнитель".into(),
                            image_url: image_url(&artist.images),
                            total: tracks.len() as u32,
                            tracks: tracks.iter().map(map_track).collect(),
                        })
                    }
                    "show" => {
                        let (show, episodes) =
                            tokio::try_join!(api.show(&id), api.show_episodes(&id, 0, 50))
                                .map_err(|error| error.to_string())?;
                        Ok(LivePlaylist {
                            id: show.id.clone(),
                            uri: show.uri.clone(),
                            title: show.name.clone(),
                            description: show.description.clone(),
                            owner: show.publisher.clone(),
                            image_url: image_url(&show.images),
                            total: episodes.total,
                            tracks: episodes
                                .items
                                .iter()
                                .map(|episode| map_episode(episode, Some(&show)))
                                .collect(),
                        })
                    }
                    _ => Err(format!("Неподдерживаемый раздел: {kind}")),
                }
            }
            .await;
            match result {
                Ok(playlist) => {
                    let mut guard = lock(&state);
                    guard.snapshot.opened_playlist = Some(playlist);
                    guard.snapshot.busy = false;
                    guard.snapshot.error = None;
                    bump(&mut guard.snapshot);
                }
                Err(error) => finish_error(&state, format!("Плейлист не загружен: {error}")),
            }
        });
    }

    pub fn command(&self, action: String, value: String) {
        if let Some(engine) = self.engine()
            && !matches!(action.as_str(), "save" | "unsave" | "transfer")
        {
            self.optimistic_command(&action, &value);
            let command = match action.as_str() {
                "play" => (!matches!(
                    engine.state_snapshot().playback,
                    Playback::Playing | Playback::Loading
                ))
                .then_some(PlayerCommand::Toggle),
                "pause" => matches!(
                    engine.state_snapshot().playback,
                    Playback::Playing | Playback::Loading
                )
                .then_some(PlayerCommand::Toggle),
                "next" => Some(PlayerCommand::Next),
                "previous" => Some(PlayerCommand::Previous),
                "seek" => value.parse().ok().map(PlayerCommand::Seek),
                "volume" => value.parse::<u16>().ok().map(|percent| {
                    PlayerCommand::Volume(
                        ((u32::from(percent.min(100)) * u32::from(u16::MAX)) / 100) as u16,
                    )
                }),
                "shuffle" => Some(PlayerCommand::Shuffle(value == "true")),
                "repeat" => Some(PlayerCommand::Repeat(RepeatMode::from_api(&value))),
                "queue" => Some(PlayerCommand::AddToQueue(value.clone())),
                "clear_queue" => Some(PlayerCommand::ClearQueue),
                "play_uri" => Some(PlayerCommand::Load(load_spec_for_uri(&value))),
                "play_context" => value.split_once('\n').map(|(context, offset)| {
                    PlayerCommand::Load(load_spec_for_context(context, offset))
                }),
                _ => None,
            };
            let result = if command.is_none() && matches!(action.as_str(), "play" | "pause") {
                Ok(())
            } else {
                command
                    .ok_or_else(|| format!("Неизвестная команда {action}"))
                    .and_then(|command| engine.command(command).map_err(|error| error.to_string()))
            };
            let mut state = lock(&self.state);
            state.snapshot.busy = false;
            if let Err(error) = result {
                state.snapshot.local_error = Some(error);
            }
            bump(&mut state.snapshot);
            return;
        }
        let Some(api) = self.api() else {
            return;
        };
        self.optimistic_command(&action, &value);
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            let result = match action.as_str() {
                "play" => api.play(None, None).await,
                "pause" => api.pause(None).await,
                "next" => api.next(None).await,
                "previous" => api.previous(None).await,
                "seek" => match value.parse::<u32>() {
                    Ok(position) => api.seek(position, None).await,
                    Err(_) => Err(crate::api::client::ApiError::Decode(
                        "invalid seek value".into(),
                    )),
                },
                "volume" => match value.parse::<u8>() {
                    Ok(volume) => api.set_volume(volume, None).await,
                    Err(_) => Err(crate::api::client::ApiError::Decode(
                        "invalid volume".into(),
                    )),
                },
                "shuffle" => api.set_shuffle(value == "true", None).await,
                "repeat" => api.set_repeat(&value, None).await,
                "queue" => api.add_to_queue(&value, None).await,
                "save" => api.save(&[value]).await,
                "unsave" => api.unsave(&[value]).await,
                "transfer" => api.transfer(&value, true).await,
                "play_uri" => {
                    let request = if value.starts_with("spotify:track:")
                        || value.starts_with("spotify:episode:")
                    {
                        PlayRequest::tracks(vec![value])
                    } else {
                        PlayRequest::context(value)
                    };
                    api.play(None, Some(&request)).await
                }
                "play_context" => match value.split_once('\n') {
                    Some((context, offset)) => {
                        let request = PlayRequest::context(context.to_string())
                            .starting_at_uri(offset.to_string());
                        api.play(None, Some(&request)).await
                    }
                    None => Err(crate::api::client::ApiError::Decode(
                        "invalid playback context".into(),
                    )),
                },
                _ => Err(crate::api::client::ApiError::Decode(format!(
                    "unknown action {action}"
                ))),
            };
            if let Err(error) = result {
                finish_error(&state, format!("Команда Spotify не выполнена: {error}"));
            } else {
                let mut guard = lock(&state);
                guard.snapshot.busy = false;
                guard.snapshot.error = None;
                bump(&mut guard.snapshot);
            }
        });
    }

    pub fn play_track(&self, track: LiveTrack) {
        {
            let mut state = lock(&self.state);
            let now_playing =
                optimistic_now_playing(track.clone(), state.snapshot.now_playing.as_ref());
            state.snapshot.now_playing = Some(now_playing);
            bump(&mut state.snapshot);
        }
        self.command("play_uri".into(), track.uri);
    }

    pub fn play_context(&self, track: LiveTrack, context_uri: String) {
        let offset_uri = track.uri.clone();
        {
            let mut state = lock(&self.state);
            let now_playing = optimistic_now_playing(track, state.snapshot.now_playing.as_ref());
            state.snapshot.now_playing = Some(now_playing);
            bump(&mut state.snapshot);
        }
        self.command(
            "play_context".into(),
            format!("{context_uri}\n{offset_uri}"),
        );
    }

    pub fn queue_track(&self, track: LiveTrack) {
        {
            let mut state = lock(&self.state);
            state.snapshot.queue.push(track.clone());
            bump(&mut state.snapshot);
        }
        self.command("queue".into(), track.uri);
    }

    pub fn update_settings(&self, json: &str) -> Result<()> {
        let settings: MobileSettings =
            serde_json::from_str(json).context("invalid Android settings")?;
        if !matches!(settings.bitrate_kbps, 96 | 160 | 320) || settings.cache_mb > 8192 {
            anyhow::bail!("unsupported playback settings");
        }
        save_settings(&self.files_dir, &settings)?;
        let had_engine = {
            let mut state = lock(&self.state);
            state.snapshot.settings = settings.clone();
            let engine = state.engine.take();
            bump(&mut state.snapshot);
            engine
        };
        if let Some(engine) = had_engine {
            engine.shutdown();
            let config = playback_config(&self.files_dir, &settings);
            if let Ok(cache) = config.open_cache()
                && let Some(credentials) = cache.credentials()
            {
                let state = Arc::clone(&self.state);
                self.runtime.spawn(async move {
                    if let Err(error) =
                        connect_engine(config, credentials, cache, Arc::clone(&state)).await
                    {
                        let mut guard = lock(&state);
                        guard.snapshot.local_playback = LocalPlaybackState::SignedOut;
                        guard.snapshot.local_error =
                            Some(format!("Настройки воспроизведения: {error}"));
                        bump(&mut guard.snapshot);
                    }
                });
            }
        }
        Ok(())
    }

    pub fn create_playlist(&self, name: String) {
        let Some(api) = self.api() else { return };
        self.set_busy();
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            match api
                .create_playlist(name.trim(), false, "Создано в Fastpotify Android")
                .await
            {
                Ok(playlist) => {
                    let mut guard = lock(&state);
                    guard.snapshot.playlists.insert(0, map_playlist(&playlist));
                    guard.snapshot.busy = false;
                    guard.snapshot.error = None;
                    bump(&mut guard.snapshot);
                }
                Err(error) => finish_error(&state, format!("Плейлист не создан: {error}")),
            }
        });
    }

    pub fn add_to_playlist(&self, playlist_id: String, uri: String) {
        let Some(api) = self.api() else { return };
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            if let Err(error) = api.add_playlist_items(&playlist_id, &[uri], None).await {
                finish_error(&state, format!("Трек не добавлен: {error}"));
            }
        });
    }

    pub fn remove_from_playlist(&self, playlist_id: String, uri: String) {
        let Some(api) = self.api() else { return };
        {
            let mut guard = lock(&self.state);
            if let Some(opened) = guard.snapshot.opened_playlist.as_mut()
                && opened.id == playlist_id
            {
                opened.tracks.retain(|track| track.uri != uri);
                opened.total = opened.total.saturating_sub(1);
            }
            bump(&mut guard.snapshot);
        }
        let state = Arc::clone(&self.state);
        self.runtime.spawn(async move {
            if let Err(error) = api.remove_playlist_items(&playlist_id, &[uri], None).await {
                finish_error(&state, format!("Трек не удалён: {error}"));
            }
        });
    }

    fn api(&self) -> Option<Arc<ApiClient>> {
        lock(&self.state).api.clone()
    }

    fn engine(&self) -> Option<Arc<Engine>> {
        let state = lock(&self.state);
        (state.snapshot.local_playback == LocalPlaybackState::Connected)
            .then(|| state.engine.clone())
            .flatten()
    }

    fn set_busy(&self) {
        let mut state = lock(&self.state);
        state.snapshot.busy = true;
        state.snapshot.error = None;
        bump(&mut state.snapshot);
    }

    fn optimistic_command(&self, action: &str, value: &str) {
        let mut state = lock(&self.state);
        state.snapshot.busy = true;
        state.snapshot.error = None;
        let queued_next = if action == "next" && !state.snapshot.queue.is_empty() {
            Some(state.snapshot.queue.remove(0))
        } else {
            None
        };
        if let Some(now) = state.snapshot.now_playing.as_mut() {
            match action {
                "play" => now.playing = true,
                "pause" => now.playing = false,
                "next" => {
                    if let Some(next) = queued_next {
                        now.track = next;
                        now.position_ms = 0;
                    }
                }
                "seek" => {
                    if let Ok(position) = value.parse() {
                        now.position_ms = position;
                    }
                }
                "shuffle" => now.shuffled = value == "true",
                "repeat" => now.repeat = value.to_string(),
                _ => {}
            }
        }
        bump(&mut state.snapshot);
    }
}

struct DashboardData {
    user: LiveUser,
    playlists: Vec<LiveCard>,
    saved_tracks: Vec<LiveTrack>,
    library_items: Vec<LiveCard>,
    made_for_you: Vec<LiveCard>,
    top_artists: Vec<LiveCard>,
    top_tracks: Vec<LiveTrack>,
    recommendations: Vec<LiveTrack>,
    recent_tracks: Vec<LiveTrack>,
    now_playing: Option<LiveNowPlaying>,
    queue: Vec<LiveTrack>,
    devices: Vec<LiveDevice>,
}

async fn fetch_dashboard(api: &Arc<ApiClient>) -> std::result::Result<DashboardData, String> {
    let user = api.me().await.map_err(|error| error.to_string())?;
    let (
        playlists,
        saved,
        albums,
        artists,
        shows,
        episodes,
        top,
        top_artists,
        recent,
        playback,
        queue,
        devices,
        discover_weekly,
        release_radar,
        daily_mix,
        daylist,
    ) = tokio::join!(
        api.my_playlists(0, 50),
        api.saved_tracks(0, 50),
        api.saved_albums(0, 50),
        api.followed_artists(None, 50),
        api.saved_shows(0, 50),
        api.saved_episodes(0, 50),
        api.top_tracks("medium_term", 20, 0),
        api.top_artists("medium_term", 20),
        api.recently_played(30, None, None),
        api.playback_state(),
        api.queue(),
        api.devices(),
        api.search("Discover Weekly", &["playlist"]),
        api.search("Release Radar", &["playlist"]),
        api.search("Daily Mix", &["playlist"]),
        api.search("daylist", &["playlist"]),
    );
    let top_tracks = top.unwrap_or_default().items;
    let seed_tracks: Vec<String> = top_tracks
        .iter()
        .filter_map(|track| track.id.clone())
        .take(5)
        .collect();
    let recommendations = if seed_tracks.is_empty() {
        Vec::new()
    } else {
        api.recommendations(&seed_tracks, &[], 20)
            .await
            .unwrap_or_default()
    };
    let mut library_items = Vec::new();
    library_items.extend(
        albums
            .unwrap_or_default()
            .items
            .iter()
            .map(|saved| map_album(&saved.album)),
    );
    library_items.extend(artists.unwrap_or_default().items.iter().map(map_artist));
    library_items.extend(
        shows
            .unwrap_or_default()
            .items
            .iter()
            .map(|saved| map_show(&saved.show)),
    );
    library_items.extend(
        episodes
            .unwrap_or_default()
            .items
            .iter()
            .map(|saved| LiveCard {
                id: saved.episode.id.clone(),
                uri: saved.episode.uri.clone(),
                title: saved.episode.name.clone(),
                subtitle: "Эпизод".into(),
                image_url: image_url(&saved.episode.images),
                kind: "episode".into(),
            }),
    );
    Ok(DashboardData {
        user: map_user(&user),
        playlists: playlists
            .unwrap_or_default()
            .items
            .iter()
            .map(map_playlist)
            .collect(),
        saved_tracks: saved
            .unwrap_or_default()
            .items
            .iter()
            .map(|saved| map_track(&saved.track))
            .collect(),
        library_items,
        made_for_you: made_for_you([
            ("Discover Weekly", discover_weekly.ok()),
            ("Release Radar", release_radar.ok()),
            ("Daily Mix", daily_mix.ok()),
            ("daylist", daylist.ok()),
        ]),
        top_artists: top_artists
            .unwrap_or_default()
            .items
            .iter()
            .map(map_artist)
            .collect(),
        top_tracks: top_tracks.iter().map(map_track).collect(),
        recommendations: recommendations.iter().map(map_track).collect(),
        recent_tracks: recent
            .unwrap_or_default()
            .items
            .iter()
            .map(|played| map_track(&played.track))
            .collect(),
        now_playing: playback.ok().flatten().and_then(map_playback),
        queue: queue
            .map(|queue| queue.queue)
            .unwrap_or_default()
            .iter()
            .map(map_playable)
            .collect(),
        devices: devices.unwrap_or_default().iter().map(map_device).collect(),
    })
}

fn apply_dashboard(snapshot: &mut LiveSnapshot, data: DashboardData) {
    snapshot.user = Some(data.user);
    snapshot.playlists = data.playlists;
    snapshot.saved_tracks = data.saved_tracks;
    snapshot.library_items = data.library_items;
    snapshot.made_for_you = data.made_for_you;
    snapshot.top_artists = data.top_artists;
    snapshot.top_tracks = data.top_tracks;
    snapshot.recommendations = data.recommendations;
    snapshot.recent_tracks = data.recent_tracks;
    snapshot.now_playing = data.now_playing;
    snapshot.queue = data.queue;
    snapshot.devices = data.devices;
    snapshot.busy = false;
    snapshot.error = None;
    bump(snapshot);
}

fn playback_config(files_dir: &Path, settings: &MobileSettings) -> EngineConfig {
    let playback_dir = files_dir.join("librespot");
    EngineConfig {
        device_name: "Fastpotify Android".into(),
        bitrate_kbps: settings.bitrate_kbps,
        normalisation: settings.normalisation,
        autoplay: settings.autoplay,
        gapless: settings.gapless,
        backend: None,
        audio_device: None,
        initial_volume: u16::MAX / 2,
        credentials_dir: playback_dir.join("credentials"),
        volume_dir: playback_dir.join("volume"),
        audio_cache_dir: (settings.cache_mb > 0).then(|| playback_dir.join("audio")),
        audio_cache_limit: (settings.cache_mb > 0)
            .then_some(u64::from(settings.cache_mb) * 1024 * 1024),
        buffer_ms: crate::sink::DEFAULT_BUFFER_MS,
        tap: crate::vis::AudioTap::new(),
        eq: crate::eq::shared(),
    }
}

fn load_settings(files_dir: &Path) -> MobileSettings {
    std::fs::read(files_dir.join(SETTINGS_FILE))
        .ok()
        .and_then(|bytes| serde_json::from_slice(&bytes).ok())
        .unwrap_or_default()
}

fn save_settings(files_dir: &Path, settings: &MobileSettings) -> Result<()> {
    let path = files_dir.join(SETTINGS_FILE);
    let temporary = files_dir.join(format!("{SETTINGS_FILE}.tmp"));
    let bytes = serde_json::to_vec_pretty(settings)?;
    std::fs::write(&temporary, bytes).context("unable to write playback settings")?;
    std::fs::rename(&temporary, &path).context("unable to replace playback settings")?;
    Ok(())
}

async fn connect_engine(
    config: EngineConfig,
    credentials: Credentials,
    cache: Cache,
    state: Arc<Mutex<State>>,
) -> Result<()> {
    {
        let mut guard = lock(&state);
        guard.snapshot.local_playback = LocalPlaybackState::Connecting;
        guard.snapshot.local_error = None;
        bump(&mut guard.snapshot);
    }
    let notify_state = Arc::clone(&state);
    let notify = Arc::new(move |event| {
        let mut guard = lock(&notify_state);
        match event {
            EngineEvent::State(local) => apply_local_state(&mut guard.snapshot, &local),
            EngineEvent::SessionEnded => {
                guard.snapshot.local_playback = LocalPlaybackState::SignedOut;
                guard.snapshot.local_error = Some("Сессия Spotify Connect завершилась".into());
            }
        }
        bump(&mut guard.snapshot);
    });
    let engine = Arc::new(Engine::connect(&config, credentials, cache, notify).await?);
    let mut guard = lock(&state);
    guard.engine = Some(engine);
    guard.snapshot.local_playback = LocalPlaybackState::Connected;
    guard.snapshot.local_error = None;
    bump(&mut guard.snapshot);
    Ok(())
}

fn apply_local_state(snapshot: &mut LiveSnapshot, local: &LocalState) {
    snapshot.local_playback = if local.connected {
        LocalPlaybackState::Connected
    } else {
        LocalPlaybackState::Connecting
    };
    snapshot.local_error = local.error.clone();
    if let Some(track) = &local.track {
        snapshot.now_playing = Some(LiveNowPlaying {
            track: LiveTrack {
                id: track.uri.clone(),
                uri: track.uri.clone(),
                title: track.title.clone(),
                artist: track.artist_names(),
                album: track.album.clone(),
                image_url: track.art_url.clone(),
                duration_ms: track.duration_ms,
                explicit: false,
            },
            position_ms: local.position_now(),
            playing: matches!(local.playback, Playback::Playing | Playback::Loading),
            shuffled: local.shuffle,
            repeat: local.repeat.api_name().into(),
            device_id: None,
        });
    }
}

fn api_client(http: &reqwest::Client, token: StoredToken, path: &Path) -> Arc<ApiClient> {
    let client = Arc::new(ApiClient::new(
        http.clone(),
        Arc::new(NetActivity::default()),
        20,
        20,
        ApiSource::Shared,
    ));
    client.set_token_provider(Some(TokenProvider::Web(WebTokens::new(
        http.clone(),
        token,
        path.to_path_buf(),
        ApiSource::Shared,
    ))));
    client
}

fn map_user(user: &User) -> LiveUser {
    LiveUser {
        id: user.id.clone(),
        name: user.name().to_string(),
        image_url: image_url(&user.images),
        product: user.product.clone(),
    }
}

fn map_playlist(playlist: &Playlist) -> LiveCard {
    LiveCard {
        id: playlist.id.clone(),
        uri: playlist.uri.clone(),
        title: playlist.name.clone(),
        subtitle: format!(
            "{} · {} треков",
            playlist.owner_name(),
            playlist.track_total()
        ),
        image_url: image_url(&playlist.images),
        kind: "playlist".into(),
    }
}

fn map_track(track: &Track) -> LiveTrack {
    LiveTrack {
        id: track.id.clone().unwrap_or_else(|| track.uri.clone()),
        uri: track.uri.clone(),
        title: track.name.clone(),
        artist: track.artist_names(),
        album: track
            .album
            .as_ref()
            .map(|album| album.name.clone())
            .unwrap_or_default(),
        image_url: track.image(IMAGE_TARGET).map(str::to_string),
        duration_ms: track.duration_ms,
        explicit: track.explicit,
    }
}

fn map_album(album: &crate::api::models::Album) -> LiveCard {
    LiveCard {
        id: album.id.clone(),
        uri: album.uri.clone(),
        title: album.name.clone(),
        subtitle: album
            .artists
            .iter()
            .map(|artist| artist.name.as_str())
            .collect::<Vec<_>>()
            .join(", "),
        image_url: image_url(&album.images),
        kind: "album".into(),
    }
}

fn map_artist(artist: &crate::api::models::Artist) -> LiveCard {
    LiveCard {
        id: artist.id.clone(),
        uri: artist.uri.clone(),
        title: artist.name.clone(),
        subtitle: "Исполнитель".into(),
        image_url: image_url(&artist.images),
        kind: "artist".into(),
    }
}

fn map_show(show: &crate::api::models::Show) -> LiveCard {
    LiveCard {
        id: show.id.clone(),
        uri: show.uri.clone(),
        title: show.name.clone(),
        subtitle: show.publisher.clone(),
        image_url: image_url(&show.images),
        kind: "show".into(),
    }
}

fn map_album_track(track: &Track, album: &crate::api::models::Album) -> LiveTrack {
    let mut mapped = map_track(track);
    mapped.album = album.name.clone();
    if mapped.image_url.is_none() {
        mapped.image_url = image_url(&album.images);
    }
    mapped
}

fn map_episode(
    episode: &crate::api::models::Episode,
    show: Option<&crate::api::models::Show>,
) -> LiveTrack {
    LiveTrack {
        id: episode.id.clone(),
        uri: episode.uri.clone(),
        title: episode.name.clone(),
        artist: show
            .map(|show| show.name.clone())
            .or_else(|| episode.show.as_ref().map(|show| show.name.clone()))
            .unwrap_or_default(),
        album: show
            .map(|show| show.name.clone())
            .or_else(|| episode.show.as_ref().map(|show| show.name.clone()))
            .unwrap_or_default(),
        image_url: image_url(&episode.images)
            .or_else(|| show.and_then(|show| image_url(&show.images))),
        duration_ms: episode.duration_ms,
        explicit: episode.explicit,
    }
}

fn map_playable(item: &PlayableItem) -> LiveTrack {
    match item {
        PlayableItem::Track(track) => map_track(track),
        PlayableItem::Episode(episode) => map_episode(episode, episode.show.as_ref()),
    }
}

fn map_playback(playback: PlaybackState) -> Option<LiveNowPlaying> {
    let track = playback.item.as_ref().map(map_playable)?;
    Some(LiveNowPlaying {
        track,
        position_ms: playback.progress_ms.unwrap_or(0),
        playing: playback.is_playing,
        shuffled: playback.shuffle_state,
        repeat: playback.repeat_state,
        device_id: playback.device.and_then(|device| device.id),
    })
}

fn map_device(device: &Device) -> LiveDevice {
    LiveDevice {
        id: device.id.clone(),
        name: device.name.clone(),
        kind: device.kind.clone(),
        active: device.is_active,
        restricted: device.is_restricted,
        volume_percent: device.volume_percent,
    }
}

fn load_spec_for_uri(uri: &str) -> LoadSpec {
    // Spotify resolves a single item as a context. This is the same path the
    // desktop client uses and lets librespot continue with radio/autoplay.
    LoadSpec {
        context_uri: Some(uri.to_string()),
        play: true,
        ..LoadSpec::default()
    }
}

fn optimistic_now_playing(track: LiveTrack, previous: Option<&LiveNowPlaying>) -> LiveNowPlaying {
    LiveNowPlaying {
        track,
        position_ms: 0,
        playing: true,
        shuffled: previous.is_some_and(|now| now.shuffled),
        repeat: previous
            .map(|now| now.repeat.clone())
            .unwrap_or_else(|| "off".into()),
        device_id: previous.and_then(|now| now.device_id.clone()),
    }
}

fn load_spec_for_context(context_uri: &str, offset_uri: &str) -> LoadSpec {
    LoadSpec {
        context_uri: Some(context_uri.to_string()),
        offset_uri: Some(offset_uri.to_string()),
        play: true,
        ..LoadSpec::default()
    }
}

fn made_for_you<const N: usize>(searches: [(&str, Option<SearchResults>); N]) -> Vec<LiveCard> {
    let mut cards = Vec::new();
    let mut seen = std::collections::HashSet::new();
    for (term, results) in searches {
        let Some(playlists) = results.and_then(|results| results.playlists) else {
            continue;
        };
        for playlist in playlists.items {
            let owner = playlist.owner.id.as_deref().unwrap_or_default();
            let key = playlist.name.trim().to_lowercase();
            if is_made_for_you(&playlist.name, term)
                && (owner == "spotify" || playlist.owner_name() == "Spotify")
                && seen.insert(key)
            {
                cards.push(map_playlist(&playlist));
            }
        }
    }
    cards
}

fn is_made_for_you(name: &str, term: &str) -> bool {
    let name = name.trim().to_lowercase();
    let term = term.to_lowercase();
    name == term
        || (term == "daily mix"
            && name.strip_prefix("daily mix ").is_some_and(|number| {
                !number.is_empty() && number.chars().all(|character| character.is_ascii_digit())
            }))
}

fn map_search(results: SearchResults) -> Vec<LiveCard> {
    let mut cards = Vec::new();
    if let Some(page) = results.tracks {
        cards.extend(page.items.iter().map(|track| LiveCard {
            id: track.id.clone().unwrap_or_else(|| track.uri.clone()),
            uri: track.uri.clone(),
            title: track.name.clone(),
            subtitle: track.artist_names(),
            image_url: track.image(IMAGE_TARGET).map(str::to_string),
            kind: "track".into(),
        }));
    }
    if let Some(page) = results.playlists {
        cards.extend(page.items.iter().map(map_playlist));
    }
    if let Some(page) = results.albums {
        cards.extend(page.items.iter().map(map_album));
    }
    if let Some(page) = results.artists {
        cards.extend(page.items.iter().map(map_artist));
    }
    if let Some(page) = results.shows {
        cards.extend(page.items.iter().map(map_show));
    }
    if let Some(page) = results.episodes {
        cards.extend(page.items.iter().map(|episode| {
            LiveCard {
                id: episode.id.clone(),
                uri: episode.uri.clone(),
                title: episode.name.clone(),
                subtitle: episode
                    .show
                    .as_ref()
                    .map(|show| show.name.clone())
                    .unwrap_or_default(),
                image_url: image_url(&episode.images),
                kind: "episode".into(),
            }
        }));
    }
    cards
}

fn image_url(images: &[Image]) -> Option<String> {
    pick_image(images, IMAGE_TARGET).map(str::to_string)
}

fn finish_error(state: &Arc<Mutex<State>>, error: impl Into<String>) {
    let mut state = lock(state);
    state.snapshot.busy = false;
    state.snapshot.error = Some(error.into());
    bump(&mut state.snapshot);
}

fn bump(snapshot: &mut LiveSnapshot) {
    snapshot.revision = snapshot.revision.saturating_add(1);
}

fn lock<T>(mutex: &Mutex<T>) -> std::sync::MutexGuard<'_, T> {
    mutex
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn track_mapping_keeps_real_artwork_and_identity() {
        let track = Track {
            id: Some("track-id".into()),
            name: "Song".into(),
            uri: "spotify:track:track-id".into(),
            album: Some(crate::api::models::Album {
                name: "Album".into(),
                images: vec![Image {
                    url: "https://i.scdn.co/image/example".into(),
                    width: Some(300),
                    height: Some(300),
                }],
                ..Default::default()
            }),
            ..Default::default()
        };
        let mapped = map_track(&track);
        assert_eq!(mapped.id, "track-id");
        assert_eq!(mapped.album, "Album");
        assert_eq!(
            mapped.image_url.as_deref(),
            Some("https://i.scdn.co/image/example")
        );
    }

    #[test]
    fn signed_out_snapshot_never_contains_credentials() {
        let snapshot = LiveSnapshot {
            contract_version: crate::CONTRACT_VERSION,
            ..Default::default()
        };
        let json = serde_json::to_string(&snapshot).unwrap();
        assert!(!json.contains("access_token"));
        assert!(!json.contains("refresh_token"));
    }

    #[test]
    fn mobile_settings_are_written_and_restored() {
        let directory =
            std::env::temp_dir().join(format!("fastpotify-mobile-settings-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&directory);
        std::fs::create_dir_all(&directory).unwrap();
        let expected = MobileSettings {
            bitrate_kbps: 160,
            normalisation: false,
            autoplay: false,
            gapless: true,
            cache_mb: 512,
        };
        save_settings(&directory, &expected).unwrap();
        assert_eq!(load_settings(&directory), expected);
        assert!(!directory.join(format!("{SETTINGS_FILE}.tmp")).exists());
        std::fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn album_tracks_inherit_collection_artwork() {
        let album = crate::api::models::Album {
            name: "Album".into(),
            images: vec![Image {
                url: "https://i.scdn.co/image/album".into(),
                width: Some(300),
                height: Some(300),
            }],
            ..Default::default()
        };
        let mapped = map_album_track(
            &Track {
                name: "Song".into(),
                ..Default::default()
            },
            &album,
        );
        assert_eq!(mapped.album, "Album");
        assert_eq!(
            mapped.image_url.as_deref(),
            Some("https://i.scdn.co/image/album")
        );
    }

    #[test]
    fn a_single_track_uses_spotifys_context_resolver() {
        let spec = load_spec_for_uri("spotify:track:abc");
        assert_eq!(spec.context_uri.as_deref(), Some("spotify:track:abc"));
        assert!(spec.uris.is_empty());
        assert!(spec.play);

        let playlist = load_spec_for_context("spotify:playlist:playlist", "spotify:track:abc");
        assert_eq!(
            playlist.context_uri.as_deref(),
            Some("spotify:playlist:playlist")
        );
        assert_eq!(playlist.offset_uri.as_deref(), Some("spotify:track:abc"));
    }

    #[test]
    fn made_for_you_keeps_spotify_mixes_and_removes_duplicates() {
        let discover: SearchResults = serde_json::from_str(
            r#"{"playlists":{"items":[{"id":"discover","uri":"spotify:playlist:discover","name":"Discover Weekly","owner":{"id":"spotify","display_name":"Spotify"}},{"id":"copy","uri":"spotify:playlist:copy","name":"Discover Weekly","owner":{"id":"someone","display_name":"Someone"}}],"total":2}}"#,
        )
        .unwrap();
        let daily: SearchResults = serde_json::from_str(
            r#"{"playlists":{"items":[{"id":"daily-1","uri":"spotify:playlist:daily-1","name":"Daily Mix 1","owner":{"id":"spotify","display_name":"Spotify"}},{"id":"not-a-mix","uri":"spotify:playlist:other","name":"Daily Mix Party","owner":{"id":"spotify","display_name":"Spotify"}}],"total":2}}"#,
        )
        .unwrap();

        let cards = made_for_you([
            ("Discover Weekly", Some(discover)),
            ("Daily Mix", Some(daily)),
        ]);

        assert_eq!(cards.len(), 2);
        assert_eq!(cards[0].title, "Discover Weekly");
        assert_eq!(cards[1].title, "Daily Mix 1");
    }
}
