use serde::{Deserialize, Serialize};

use crate::profile::{DemoScreen, UiProfile};

pub const DEMO_POSITION_MS: u64 = 94_000;
pub const DEMO_DURATION_MS: u64 = 222_000;

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct DemoSnapshot {
    pub contract_version: u32,
    pub requested_profile: UiProfile,
    pub screen: DemoScreen,
    pub avatar: String,
    pub filters: Vec<String>,
    pub quick_cards: Vec<QuickCard>,
    pub featured: FeaturedItem,
    pub tracks: Vec<Track>,
    pub now_playing: NowPlaying,
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct QuickCard {
    pub id: String,
    pub title: String,
    pub subtitle: String,
    pub palette: [u32; 2],
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct FeaturedItem {
    pub title: String,
    pub kind: String,
    pub description: String,
    pub palette: [u32; 2],
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct Track {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub metadata: String,
    pub palette: [u32; 2],
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct NowPlaying {
    pub title: String,
    pub artist: String,
    pub position_ms: u64,
    pub duration_ms: u64,
    pub liked: bool,
    pub playing: bool,
    pub shuffled: bool,
    pub repeating: bool,
    pub palette: [u32; 2],
}
