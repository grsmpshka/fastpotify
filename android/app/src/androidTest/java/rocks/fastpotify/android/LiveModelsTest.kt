package rocks.fastpotify.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import rocks.fastpotify.android.model.LiveSnapshot
import rocks.fastpotify.android.model.LocalPlaybackState

@RunWith(AndroidJUnit4::class)
class LiveModelsTest {
    @Test
    fun parsesArtworkPlaybackAndSettingsFromNativeSnapshot() {
        val snapshot = LiveSnapshot.fromJson(
            """{"revision":7,"auth_status":"signed_in","local_playback":"connected","playlists":[{"id":"p","uri":"spotify:playlist:p","title":"Mix","subtitle":"Spotify","image_url":"https://i.scdn.co/image/mix","kind":"playlist"}],"settings":{"bitrate_kbps":160,"normalisation":false,"autoplay":true,"gapless":true,"cache_mb":512}}""",
        )
        assertEquals(7, snapshot.revision)
        assertEquals(LocalPlaybackState.Connected, snapshot.localPlayback)
        assertEquals("https://i.scdn.co/image/mix", snapshot.playlists.single().imageUrl)
        assertEquals(160, snapshot.settings.bitrateKbps)
        assertTrue(snapshot.settings.gapless)
    }
}
