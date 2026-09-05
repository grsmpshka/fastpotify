The first Android preview brings Fastpotify's compact music interface to phones and wide automotive displays. It is an interface preview with deterministic demo data, not yet a Spotify sign-in or playback build.

## New

- **One APK for phone and Voyah Free.** Automatic layout selection and a manual profile chooser keep controls readable on a Galaxy S24 Ultra-sized screen and a 1920×720 automotive display.
- **A familiar adaptive music library.** Quick-access tiles, square artwork, Liked Songs, playlists, track rows, a mini player, and a persistent automotive player follow Fastpotify's desktop proportions.
- **Updates from inside the app.** Fastpotify checks this fork's Android releases once a day, verifies the downloaded APK checksum and signing certificate, and then opens Android's installer.
- **Deterministic interface coverage.** CI renders and checks five phone and automotive screenshots on an Android emulator.

## Fixed

- **Artwork stays proportional.** Covers use centered square crops and size-aware fallbacks instead of stretching or disappearing.

## Thanks

Thanks to the Fastpotify contributors whose desktop interface and shared Rust core make this Android preview possible.

Full changelog: `3a3d793...v0.6.0-android.1`
