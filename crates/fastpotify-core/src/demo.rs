use crate::{
    CONTRACT_VERSION,
    contract::{
        DEMO_DURATION_MS, DEMO_POSITION_MS, DemoSnapshot, FeaturedItem, NowPlaying, QuickCard,
        Track,
    },
    profile::{DemoScreen, UiProfile},
};

/// Returns a deterministic, fully offline snapshot for previews and CI.
pub fn demo_snapshot(profile: UiProfile, screen: DemoScreen) -> DemoSnapshot {
    DemoSnapshot {
        contract_version: CONTRACT_VERSION,
        requested_profile: profile,
        screen,
        avatar: "P".into(),
        filters: vec!["Все".into(), "Музыка".into(), "Подкасты".into()],
        quick_cards: vec![
            card("liked", "Любимые треки", "Коллекция", [0x5B2C83, 0x1ED760]),
            card("top", "Today's Top Hits", "Playlist", [0x69374A, 0xD88A9A]),
            card(
                "afternoon",
                "Saturday Afternoon",
                "Playlist",
                [0x8A5A32, 0xE7B76C],
            ),
            card("radio", "Demumanized Radio", "Radio", [0x213B55, 0x3F88C5]),
            card("2010s", "Mix 2010-х", "Mix", [0x5D384E, 0xCB6580]),
            card("daily", "Daily Mix", "Mix", [0x295348, 0x5AB89D]),
        ],
        featured: FeaturedItem {
            title: "Dreamy Forest Music".into(),
            kind: "Playlist".into(),
            description: "Wander into the magical mix of music, water, birds and calmness.".into(),
            palette: [0x102F29, 0x4D8C69],
        },
        tracks: vec![
            track(
                "track-1",
                "Test Track 1",
                "Test Artist",
                "3:42",
                [0x24433B, 0x77A88E],
            ),
            track(
                "track-2",
                "Test Track 2",
                "Example Band",
                "4:08",
                [0x493956, 0xA578B5],
            ),
            track(
                "track-3",
                "Test Track 3",
                "Demo Artist",
                "2:57",
                [0x493B29, 0xC69B5E],
            ),
        ],
        now_playing: NowPlaying {
            title: "Test Track".into(),
            artist: "Test Artist".into(),
            position_ms: DEMO_POSITION_MS,
            duration_ms: DEMO_DURATION_MS,
            liked: true,
            playing: true,
            shuffled: false,
            repeating: false,
            palette: [0x173D34, 0x70AD8D],
        },
    }
}

/// Serializes the deterministic fixture for thin platform bridges.
pub fn demo_snapshot_json(profile: UiProfile, screen: DemoScreen) -> String {
    serde_json::to_string(&demo_snapshot(profile, screen))
        .expect("the static demo snapshot is always serializable")
}

fn card(id: &str, title: &str, subtitle: &str, palette: [u32; 2]) -> QuickCard {
    QuickCard {
        id: id.into(),
        title: title.into(),
        subtitle: subtitle.into(),
        palette,
    }
}

fn track(id: &str, title: &str, artist: &str, metadata: &str, palette: [u32; 2]) -> Track {
    Track {
        id: id.into(),
        title: title.into(),
        artist: artist.into(),
        metadata: metadata.into(),
        palette,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fixture_is_stable_and_offline() {
        let snapshot = demo_snapshot(UiProfile::VoyahFree, DemoScreen::Home);
        assert_eq!(snapshot.quick_cards.len(), 6);
        assert_eq!(snapshot.now_playing.position_ms, 94_000);
        assert_eq!(snapshot.now_playing.duration_ms, 222_000);
        assert_eq!(snapshot.tracks[0].title, "Test Track 1");

        let json = demo_snapshot_json(UiProfile::Phone, DemoScreen::NowPlaying);
        assert!(!json.contains("http://"));
        assert!(!json.contains("https://"));
        assert!(json.contains("Dreamy Forest Music"));
    }
}
