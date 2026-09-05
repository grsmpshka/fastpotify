//! Platform-neutral contracts shared by Fastpotify frontends.

pub mod artwork;
pub mod contract;
pub mod demo;
pub mod profile;

pub use artwork::{ArtworkImage, pick_artwork};
pub use contract::{
    DEMO_DURATION_MS, DEMO_POSITION_MS, DemoSnapshot, FeaturedItem, NowPlaying, QuickCard, Track,
};
pub use demo::{demo_snapshot, demo_snapshot_json};
pub use profile::{DemoScreen, UiProfile};

/// Version of the frontend/core contract exposed across the JNI boundary.
pub const CONTRACT_VERSION: u32 = 1;
