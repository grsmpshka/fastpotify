This Android preview focuses on reliability. Playback setup is now explicit, repeated Spotify items no longer crash long lists, and the home page contains the personalised shelves available to Fastpotify.

## New

- **Made for You is populated.** Discover Weekly, Release Radar, Daily Mix, and daylist use the same Spotify searches and filtering rules as the desktop client.
- **More personal shelves.** The home page now shows top artists, recent listening, top tracks, and seeded recommendations as artwork cards.
- **Functional parity is tracked.** A desktop-to-Android audit now records implemented features, platform differences, known gaps, and the release checks that cover them.

## Fixed

- **Long lists stay open.** Repeated tracks in history, playlists, and the queue now receive distinct Compose identities instead of crashing when scrolled into view.
- **The first song starts after setup.** If no Spotify player is active, tapping a song starts the separate librespot authorization and remembers the request until the Android player connects.
- **Playlist playback keeps its context.** Choosing a row loads its playlist, album, or Liked Songs context at that track instead of reducing it to an isolated item.
- **Android uses the correct Connect identity.** The local receiver now registers as a smartphone and opens the same tested CPAL/rodio output used by the shared Fastpotify engine.
- **Playback errors are visible.** Local and Spotify command failures appear near the top of Home and on the player instead of below several long sections.
- **Artwork uses bounded memory.** Notification artwork is decoded at a controlled size to reduce background memory pressure.
- **Notification permission no longer interrupts sign-in.** Android asks only after the local player has connected.

## Thanks

Thanks to the user who reported the playback failure, scrolling crashes, and missing personalised playlists, and to the Fastpotify and librespot contributors whose shared client and audio engine power this release.

Full changelog: `v0.6.0-android.2...v0.6.0-android.3`
