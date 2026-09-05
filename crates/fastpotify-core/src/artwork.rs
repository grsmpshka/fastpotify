use serde::{Deserialize, Serialize};

/// One image variant returned by Spotify for an album, artist, or playlist.
#[derive(Clone, Debug, Default, Deserialize, Eq, PartialEq, Serialize)]
pub struct ArtworkImage {
    pub url: String,
    pub width: Option<u32>,
    pub height: Option<u32>,
}

/// Picks the smallest image that is still large enough for the rendered
/// target. If none is large enough, returns the largest available variant.
pub fn pick_artwork(images: &[ArtworkImage], target: u32) -> Option<&str> {
    let mut best: Option<&ArtworkImage> = None;
    for image in images {
        let width = image.width.unwrap_or(u32::MAX);
        match best {
            None => best = Some(image),
            Some(current) => {
                let current_width = current.width.unwrap_or(u32::MAX);
                let current_ok = current_width >= target;
                let candidate_ok = width >= target;
                let better = match (current_ok, candidate_ok) {
                    (true, true) => width < current_width,
                    (false, true) => true,
                    (true, false) => false,
                    (false, false) => width > current_width,
                };
                if better {
                    best = Some(image);
                }
            }
        }
    }
    best.map(|image| image.url.as_str())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn image(url: &str, width: Option<u32>) -> ArtworkImage {
        ArtworkImage {
            url: url.into(),
            width,
            height: width,
        }
    }

    #[test]
    fn chooses_nearest_variant_without_upscaling_when_possible() {
        let images = [
            image("large", Some(640)),
            image("small", Some(64)),
            image("medium", Some(300)),
        ];

        assert_eq!(pick_artwork(&images, 64), Some("small"));
        assert_eq!(pick_artwork(&images, 100), Some("medium"));
        assert_eq!(pick_artwork(&images, 1000), Some("large"));
        assert_eq!(pick_artwork(&[], 64), None);
    }
}
