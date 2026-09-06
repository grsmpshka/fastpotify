# Fastpotify desktop to Android functional audit

This checklist compares the repository's desktop client with the Android
client. It is maintained from the user-visible flows in `src/ui`, the actions
in `src/app.rs`, and the Android UI/native bridge. A green Android build alone
does not count as functional coverage.

| Area | Desktop Fastpotify | Android status | Verification |
| --- | --- | --- | --- |
| Shared Spotify sign-in | PKCE browser grant, persisted token | Implemented with the same auth module | Native auth unit tests and signed-out launch test |
| Personal Web API app | Optional second grant and routing | Not exposed on Android yet | Documented gap |
| Local Premium playback | Separate librespot grant | Implemented; the first requested song now waits for the grant and starts after connection | Context-routing unit tests plus Android output probe |
| Spotify Connect receiver | Local device, reconnect, transfer | Implemented; Android identifies as a smartphone | Cross-compiled engine and device UI tests |
| Background controls | Desktop media integration and tray | Android foreground service, MediaSession, notification, lock-screen and Bluetooth controls | Android service launch and lint |
| Home quick access | Liked Songs and playlists | Implemented with real artwork | Compose UI tests |
| Made for you | Discover Weekly, Release Radar, Daily Mix and daylist | Implemented from the same four Spotify searches and filtering rules | Rust filtering regression test |
| Recent listening | Spotify plus local history | Spotify recent history is implemented; Android-local history is not yet persisted | Snapshot parser and live UI tests |
| Top artists and tracks | Implemented | Implemented as Spotify-style card shelves | Snapshot parser and scrolling stress test |
| Recommendations | Seeded from top tracks | Implemented with the same Web API recommendation route | Rust mapping and UI tests; availability still depends on Spotify account/app policy |
| Search | Tracks, artists, albums, playlists, shows and episodes | Implemented for all six types | Parser and collection navigation tests |
| Liked Songs | Browse, play in collection context, save/remove | Browse and contextual playback implemented; bulk operations and explicit saved-state display remain a gap | Playback-context regression test |
| Playlists | Browse, create, edit metadata, delete, reorder and edit songs | Browse, create, add and remove songs implemented. Rename, delete and reorder remain gaps | Native API tests and duplicate-row stress test |
| Albums | Browse, save and play | Browse and contextual playback implemented; explicit save/remove UI remains a gap | Collection UI tests |
| Artists | Top tracks, releases and related artists | Artist top tracks implemented. Releases and related artists remain gaps | Collection UI tests |
| Podcasts | Shows and episodes | Browse and episode playback implemented | Snapshot parser and collection UI tests |
| Queue | Manual queue plus context queue, persistence and history tab | Add, inspect, play and clear commands exist. Desktop's complete persistence/history rules are not yet ported | Duplicate-entry stress test; desktop queue contract remains authoritative |
| Core controls | Play, pause, next, previous, seek, volume, shuffle and repeat | Implemented locally and for remote devices | Rust command tests, MediaSession callbacks and Android UI tests |
| Links | `spotify:` and `open.spotify.com` | Implemented with Android deep links | URI parser coverage |
| Updates | GitHub update check | Signed APK download, checksum and certificate verification implemented | Updater instrumentation tests |
| Lyrics | Spotify and LRCLIB fallback | Not exposed on Android yet | Documented gap |
| Equalizer, mono and balance | Implemented in the shared audio path | Audio path is shared, but Android controls are not exposed yet | Documented gap |
| Visualizers, Winamp and MilkDrop | Desktop-specific windows | Intentionally desktop-only; not suitable for the Android shell | Out of Android UI scope |
| Keyboard shortcuts, window chrome and tray | Desktop platform integration | Replaced by Android navigation, MediaSession and system lifecycle | Platform-specific by design |

## Regression gates for Android releases

1. Compile the complete Rust workspace on Linux and cross-compile both Android
   ABIs.
2. Run shared auth, API, queue, audio and mobile snapshot unit tests.
3. Run Android lint and Kotlin compilation.
4. Open Android's real CPAL/rodio output on an emulator.
5. Scroll repeated recent, recommendation, playlist and queue entries. Spotify
   permits duplicates, so duplicate data must never crash Compose.
6. Capture phone and 1920x720 automotive screens.
7. Install the signed APK over the previous release and verify the package,
   version, ABI set, SHA-256 and signing certificate.

Items marked as gaps must not be described as implemented in the README or
release notes until their UI and regression coverage land.
