This Android preview completes the browser authorization handoff, restores local playback, and keeps useful Spotify content visible while the library refreshes.

## Fixed

- **The selected song starts after playback authorization.** The pending song now lives in the native core, survives the browser round trip and Activity recreation, and is sent to librespot after the phone registers as a Spotify Connect device.
- **Fast Spotify redirects no longer miss the app.** The loopback callback is listening before Chrome opens, so an already signed-in Spotify session cannot race the local authorization handler and end on `ERR_CONNECTION_REFUSED`.
- **Android returns from Chrome before exchanging the token.** The callback brings the existing Fastpotify activity to the foreground, preventing Android's cached-process freezer from timing out the token request while the browser remains open.
- **Token exchange waits for Android networking to resume.** Fastpotify now lets the foreground handoff settle before resolving Spotify's token endpoint, preventing the DNS failure reproduced in the emulator.
- **Local playback now completes Spotify authorization reliably.** Android uses librespot's bundled WebPKI roots instead of a native CA store that Android does not expose to Rust. It retains the OAuth bearer granted for streaming, refreshes it before expiry, and restores playback after an app restart.
- **Spotify connection failures stop spinning.** Web requests have finite connection and request deadlines, and a stalled librespot connection reports an actionable error after 45 seconds.
- **Sign-in reaches the app sooner.** A successful OAuth grant enters the signed-in interface before optional home data finishes loading.
- **Search remains usable during refresh.** A slow home refresh no longer disables the search action.
- **Previously loaded content survives a restart.** The last successful credential-free library snapshot is shown while fresh Spotify data loads.

## New

- **Home and search no longer start empty.** Saved tracks fill recommendation and search shelves when Spotify's optional personalization endpoints return nothing.
- **Real-device failures are diagnosable.** Native Spotify and librespot errors are now available in Android logcat without recording credentials.

## Thanks

Thanks to the user who supplied the real-device recording that exposed the lost playback command and network stalls, and to the Fastpotify and librespot contributors whose shared client and playback engine power the app.

Full changelog: `v0.6.0-android.4...v0.6.0-android.10`
