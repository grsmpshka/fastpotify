//! JNI boundary for the Fastpotify Android application.

use std::panic::{AssertUnwindSafe, catch_unwind};
use std::sync::{Mutex, OnceLock};

use fastpotify_core::{DemoScreen, UiProfile, demo_snapshot_json, live::MobileClient};
use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JObject, JString},
    sys::{jint, jstring},
};

static MOBILE_CLIENT: OnceLock<Mutex<Option<MobileClient>>> = OnceLock::new();
static ANDROID_CONTEXT: OnceLock<GlobalRef> = OnceLock::new();

fn mobile_client() -> &'static Mutex<Option<MobileClient>> {
    MOBILE_CLIENT.get_or_init(|| Mutex::new(None))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_contractVersion(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    fastpotify_core::CONTRACT_VERSION as jint
}

#[cfg(all(target_os = "android", debug_assertions))]
#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_audioProbe(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    string_result(&mut env, || {
        fastpotify_core::sink::probe_output().map_err(anyhow::Error::msg)
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_demoSnapshotJson(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    profile: JString<'_>,
    screen: JString<'_>,
) -> jstring {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let profile = env.get_string(&profile)?.to_string_lossy().into_owned();
        let screen = env.get_string(&screen)?.to_string_lossy().into_owned();
        let json = demo_snapshot_json(parse_profile(&profile), parse_screen(&screen));
        env.new_string(json)
    }));

    match result {
        Ok(Ok(value)) => value.into_raw(),
        Ok(Err(error)) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", error.to_string());
            std::ptr::null_mut()
        }
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Fastpotify core panicked while building the demo snapshot",
            );
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_initialize(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    files_dir: JString<'_>,
    context: JObject<'_>,
) {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if ANDROID_CONTEXT.get().is_none() {
            let global = env
                .new_global_ref(&context)
                .map_err(|error| error.to_string())?;
            // CPAL uses ndk-context to reach Android's audio services. The
            // global reference keeps the application Context alive for the
            // lifetime of the native process.
            #[cfg(target_os = "android")]
            {
                let vm = env.get_java_vm().map_err(|error| error.to_string())?;
                unsafe {
                    ndk_context::initialize_android_context(
                        vm.get_java_vm_pointer().cast(),
                        global.as_obj().as_raw().cast(),
                    );
                }
            }
            let _ = ANDROID_CONTEXT.set(global);
        }
        let files_dir = env
            .get_string(&files_dir)
            .map_err(|error| error.to_string())?
            .to_string_lossy()
            .into_owned();
        let client = MobileClient::new(files_dir).map_err(|error| error.to_string())?;
        *mobile_client()
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner()) = Some(client);
        Ok::<_, String>(())
    }));
    if let Err(message) = flatten(result) {
        let _ = env.throw_new("java/lang/IllegalStateException", message);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_startLocalSignIn(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    string_result(&mut env, || {
        with_client(|client| client.start_local_sign_in())
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_liveSnapshotJson(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    string_result(&mut env, || {
        with_client(|client| Ok(client.snapshot_json()))
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_startSignIn(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    string_result(&mut env, || with_client(|client| client.start_sign_in()))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_refresh(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    let _ = with_client(|client| {
        client.refresh();
        Ok(())
    });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_search(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    query: JString<'_>,
) {
    if let Ok(query) = env.get_string(&query) {
        let query = query.to_string_lossy().into_owned();
        let _ = with_client(|client| {
            client.search(query);
            Ok(())
        });
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_openPlaylist(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    playlist_id: JString<'_>,
) {
    if let Ok(playlist_id) = env.get_string(&playlist_id) {
        let playlist_id = playlist_id.to_string_lossy().into_owned();
        let _ = with_client(|client| {
            client.open_playlist(playlist_id);
            Ok(())
        });
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_openContent(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    kind: JString<'_>,
    id: JString<'_>,
) {
    let kind = env
        .get_string(&kind)
        .map(|value| value.to_string_lossy().into_owned());
    let id = env
        .get_string(&id)
        .map(|value| value.to_string_lossy().into_owned());
    if let (Ok(kind), Ok(id)) = (kind, id) {
        let _ = with_client(|client| {
            client.open_content(kind, id);
            Ok(())
        });
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_command(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    action: JString<'_>,
    value: JString<'_>,
) {
    let action = env
        .get_string(&action)
        .map(|value| value.to_string_lossy().into_owned());
    let value = env
        .get_string(&value)
        .map(|value| value.to_string_lossy().into_owned());
    if let (Ok(action), Ok(value)) = (action, value) {
        let _ = with_client(|client| {
            client.command(action, value);
            Ok(())
        });
    }
}

fn track_command(
    env: &mut JNIEnv<'_>,
    json: &JString<'_>,
    operation: impl FnOnce(&MobileClient, fastpotify_core::live::LiveTrack),
) {
    let result = env
        .get_string(json)
        .map(|value| value.to_string_lossy().into_owned())
        .map_err(|error| anyhow::anyhow!(error))
        .and_then(|json| serde_json::from_str(&json).map_err(anyhow::Error::from))
        .and_then(|track| {
            with_client(|client| {
                operation(client, track);
                Ok(())
            })
        });
    if let Err(error) = result {
        let _ = env.throw_new("java/lang/IllegalArgumentException", error.to_string());
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_playTrack(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    json: JString<'_>,
) {
    track_command(&mut env, &json, MobileClient::play_track);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_playContext(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    json: JString<'_>,
    context_uri: JString<'_>,
) {
    let context_uri = env
        .get_string(&context_uri)
        .map(|value| value.to_string_lossy().into_owned());
    if let Ok(context_uri) = context_uri {
        track_command(&mut env, &json, |client, track| {
            client.play_context(track, context_uri);
        });
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_queueTrack(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    json: JString<'_>,
) {
    track_command(&mut env, &json, MobileClient::queue_track);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_updateSettings(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    json: JString<'_>,
) {
    let result = env
        .get_string(&json)
        .map(|value| value.to_string_lossy().into_owned())
        .map_err(|error| anyhow::anyhow!(error))
        .and_then(|json| with_client(|client| client.update_settings(&json)));
    if let Err(error) = result {
        let _ = env.throw_new("java/lang/IllegalArgumentException", error.to_string());
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_createPlaylist(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    name: JString<'_>,
) {
    if let Ok(name) = env.get_string(&name) {
        let name = name.to_string_lossy().into_owned();
        let _ = with_client(|client| {
            client.create_playlist(name);
            Ok(())
        });
    }
}

fn two_string_command(
    env: &mut JNIEnv<'_>,
    first: &JString<'_>,
    second: &JString<'_>,
    operation: impl FnOnce(&MobileClient, String, String),
) {
    let first = env
        .get_string(first)
        .map(|value| value.to_string_lossy().into_owned());
    let second = env
        .get_string(second)
        .map(|value| value.to_string_lossy().into_owned());
    if let (Ok(first), Ok(second)) = (first, second) {
        let _ = with_client(|client| {
            operation(client, first, second);
            Ok(())
        });
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_addToPlaylist(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    playlist_id: JString<'_>,
    uri: JString<'_>,
) {
    two_string_command(&mut env, &playlist_id, &uri, MobileClient::add_to_playlist);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_removeFromPlaylist(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    playlist_id: JString<'_>,
    uri: JString<'_>,
) {
    two_string_command(
        &mut env,
        &playlist_id,
        &uri,
        MobileClient::remove_from_playlist,
    );
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_signOut(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    let _ = with_client(|client| {
        client.sign_out();
        Ok(())
    });
}

fn with_client<T>(operation: impl FnOnce(&MobileClient) -> anyhow::Result<T>) -> anyhow::Result<T> {
    let guard = mobile_client()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    operation(
        guard
            .as_ref()
            .ok_or_else(|| anyhow::anyhow!("Fastpotify mobile core is not initialized"))?,
    )
}

fn string_result(
    env: &mut JNIEnv<'_>,
    operation: impl FnOnce() -> anyhow::Result<String>,
) -> jstring {
    let result = catch_unwind(AssertUnwindSafe(operation));
    match result {
        Ok(Ok(value)) => env
            .new_string(value)
            .map(|value| value.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(error)) => {
            let _ = env.throw_new("java/lang/IllegalStateException", error.to_string());
            std::ptr::null_mut()
        }
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Fastpotify core panicked",
            );
            std::ptr::null_mut()
        }
    }
}

fn flatten<T, E: std::fmt::Display>(
    result: std::thread::Result<Result<T, E>>,
) -> Result<T, String> {
    match result {
        Ok(Ok(value)) => Ok(value),
        Ok(Err(error)) => Err(error.to_string()),
        Err(_) => Err("Fastpotify core panicked".into()),
    }
}

fn parse_profile(value: &str) -> UiProfile {
    match value {
        "voyah_free" => UiProfile::VoyahFree,
        "phone" => UiProfile::Phone,
        _ => UiProfile::Automatic,
    }
}

fn parse_screen(value: &str) -> DemoScreen {
    match value {
        "playlist" => DemoScreen::Playlist,
        "now_playing" => DemoScreen::NowPlaying,
        _ => DemoScreen::Home,
    }
}
