Fastpotify for Android is now a live Spotify client rather than an interface preview. It keeps the compact native design, but the library, artwork, controls, queue, devices, and playback now come from Spotify and Fastpotify's existing Rust engine.

## New

- **Real Spotify library and artwork.** Sign in on Spotify's page and browse playlists, Liked Songs, albums, followed artists, podcasts, episodes, recent listening, and top tracks with their real covers.
- **Local Premium playback.** Fastpotify's existing librespot player runs on Android at up to 320 kbps and appears as a Spotify Connect device.
- **Background media controls.** Playback continues through a foreground service with notification, lock-screen, headset, and system media controls.
- **Full search and content pages.** Search songs, albums, artists, playlists, podcasts, and episodes, then open or play the result.
- **Queue, devices, and playlists.** Control other Connect devices, seek, change volume, shuffle and repeat, edit the queue, create playlists, and add or remove playlist tracks.
- **Playback settings.** Choose quality, normalisation, gapless playback, autoplay, and an audio-cache limit from inside the app.
- **Spotify link handling.** Open `spotify:` and `open.spotify.com` links directly in Fastpotify.
- **One adaptive APK.** The signed package covers Galaxy S24 Ultra-sized phones, arm64 devices, x86_64 systems, and the wide Voyah Free interface.
- **Safe in-app updates.** Updates are accepted only when their checksum and permanent signing certificate match this app.

## Fixed

- **Artwork keeps familiar proportions.** Covers remain square, center-cropped, and consistently spaced; Liked Songs has its dedicated gradient heart cover.
- **Playback state stays current.** Optimistic controls respond immediately while Spotify and librespot catch up in the background.

## Thanks

Thanks to the Fastpotify and librespot contributors whose existing desktop engine, Web API client, and Spotify Connect implementation are reused by this Android release.

Full changelog: `v0.6.0-android.1...v0.6.0-android.2`
