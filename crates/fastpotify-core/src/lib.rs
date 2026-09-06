//! Platform-neutral contracts shared by Fastpotify frontends.

#[path = "../../../src/api/mod.rs"]
pub mod api;
pub mod artwork;
#[path = "../../../src/auth.rs"]
pub mod auth;
pub mod contract;
pub mod demo;
#[path = "../../../src/eq.rs"]
pub mod eq;
#[path = "../../../src/limiter.rs"]
pub mod limiter;
pub mod live;
#[path = "../../../src/player.rs"]
pub mod player;
pub mod profile;
#[path = "../../../src/resample.rs"]
pub mod resample;
#[path = "../../../src/sink.rs"]
pub mod sink;
#[path = "../../../src/vis.rs"]
pub mod vis;

pub use artwork::{ArtworkImage, pick_artwork};
pub use contract::{
    DEMO_DURATION_MS, DEMO_POSITION_MS, DemoSnapshot, FeaturedItem, NowPlaying, QuickCard, Track,
};
pub use demo::{demo_snapshot, demo_snapshot_json};
pub use profile::{DemoScreen, UiProfile};

/// Version of the frontend/core contract exposed across the JNI boundary.
pub const CONTRACT_VERSION: u32 = 1;
