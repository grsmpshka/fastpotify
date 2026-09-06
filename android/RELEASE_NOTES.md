This Android preview fixes the broken Spotify request limits that prevented search and playlist pages from working, and makes local playback survive Android screen and service recreation.

## Fixed

- **Playlists open again.** Playlist pages now request Spotify's supported page size and use the compatibility endpoint when Spotify restricts the current endpoint for catalogue playlists.
- **Search returns results.** Search uses Spotify's current per-type limit, runs after typing, and retries compatible content groups if one market-specific type is rejected.
- **The library appears first.** Playlists and saved tracks no longer wait for recommendations, history, podcasts, and other optional home shelves.
- **Failures no longer look like endless loading.** A failed playlist page shows Spotify's actual error and a way back.
- **Playback survives Android recreation.** Activity and foreground-service startup reuse the existing native runtime instead of replacing the librespot session.
- **Long lists remain stable.** The scrolling stress suite still covers duplicate tracks, playlists, queue rows, and repeated recomposition.

## New

- **Search is useful before typing.** Top tracks and artists appear as initial suggestions when Spotify has loaded them.

## Thanks

Thanks to the user who reported the real-device failures and insisted that the Android port be checked against actual Spotify behavior, and to the Fastpotify and librespot contributors whose shared client and playback engine power the app.

Full changelog: `v0.6.0-android.3...v0.6.0-android.4`
