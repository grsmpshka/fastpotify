# Fastpotify for Android

This directory builds the native Android client. One APK adapts to ordinary
phones, Galaxy S24 Ultra-sized screens, landscape tablets, and wide Voyah Free
displays. The UI is Jetpack Compose; authentication, Spotify Web API access,
queue rules, audio processing, and Spotify Connect playback reuse the Rust
modules in the repository root.

## User features

- Spotify PKCE sign-in in the system browser. Fastpotify never receives the
  account password.
- Real playlists, Liked Songs, albums, artists, podcasts, episodes, recent
  history, top tracks, search results, and Spotify artwork.
- Local Premium playback through librespot, plus control and transfer of other
  Spotify Connect devices.
- Play, pause, previous, next, seek, volume, shuffle, repeat, queue, save,
  create playlist, add to playlist, and remove from playlist.
- Background playback with Android MediaSession, foreground notification, and
  lock-screen controls.
- 96, 160, or 320 kbps playback, normalisation, gapless playback, autoplay,
  and a configurable on-device audio cache.
- Automatic phone or automotive layout selection, with a manual override.
- Signed, checksum-verified updates from the fork's GitHub Releases page.

Spotify Premium is required only for local playback. Browsing and search can
still work for Free accounts. Spotify does not expose lossless playback,
offline downloads, Smart Shuffle, or local-file playback to this client, so
the Android build does not claim those capabilities.

## Build

Install JDK 17, Android SDK 35, NDK `27.2.12479018`, Rust 1.98, the Android Rust
targets, and `cargo-ndk` 3.5.4. Then run:

```sh
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk --locked --version 3.5.4
cd android
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Release builds require the four `fastpotifyStore*` Gradle properties used by
the release workflow. Never commit a keystore or its passwords.

## Verification

The Android workflow checks Rust formatting and compilation, shared-core unit
tests, both Android ABIs, Kotlin/Android lint, parser regression tests, and
deterministic phone and Voyah screenshot scenarios. Release tags matching
`v*-android.*` build the signed APK and publish it with `checksums.txt`.
