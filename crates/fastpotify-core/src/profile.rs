use serde::{Deserialize, Serialize};

/// User-selectable Android presentation profile.
#[derive(Clone, Copy, Debug, Default, Deserialize, Eq, PartialEq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum UiProfile {
    #[default]
    Automatic,
    VoyahFree,
    Phone,
}

/// Stable demo destinations used by local development and screenshot tests.
#[derive(Clone, Copy, Debug, Default, Deserialize, Eq, PartialEq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum DemoScreen {
    #[default]
    Home,
    Playlist,
    NowPlaying,
}
