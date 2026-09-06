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
            """{"revision":7,"auth_status":"signed_in","local_playback":"connected","playlists":[{"id":"p","uri":"spotify:playlist:p","title":"Mix","subtitle":"Spotify","image_url":"https://i.scdn.co/image/mix","kind":"playlist"}],"made_for_you":[{"id":"daily","uri":"spotify:playlist:daily","title":"Daily Mix 1","subtitle":"Spotify","image_url":"https://i.scdn.co/image/daily","kind":"playlist"}],"top_artists":[{"id":"artist","uri":"spotify:artist:artist","title":"Artist","subtitle":"Исполнитель","image_url":null,"kind":"artist"}],"recommendations":[{"id":"track","uri":"spotify:track:track","title":"Track","artist":"Artist","album":"Album","image_url":null,"duration_ms":1000,"explicit":false}],"settings":{"bitrate_kbps":160,"normalisation":false,"autoplay":true,"gapless":true,"cache_mb":512}}""",
        )
        assertEquals(7, snapshot.revision)
        assertEquals(LocalPlaybackState.Connected, snapshot.localPlayback)
        assertEquals("https://i.scdn.co/image/mix", snapshot.playlists.single().imageUrl)
        assertEquals("Daily Mix 1", snapshot.madeForYou.single().title)
        assertEquals("Artist", snapshot.topArtists.single().title)
        assertEquals("Track", snapshot.recommendations.single().title)
        assertEquals(160, snapshot.settings.bitrateKbps)
        assertTrue(snapshot.settings.gapless)
    }
}
