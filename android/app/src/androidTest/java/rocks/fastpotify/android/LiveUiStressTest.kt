package rocks.fastpotify.android

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.fastpotify.android.model.AuthState
import rocks.fastpotify.android.model.LiveCard
import rocks.fastpotify.android.model.LiveNowPlaying
import rocks.fastpotify.android.model.LivePlaylist
import rocks.fastpotify.android.model.LiveSnapshot
import rocks.fastpotify.android.model.LiveTrack
import rocks.fastpotify.android.model.LiveUser
import rocks.fastpotify.android.model.LocalPlaybackState
import rocks.fastpotify.android.model.MobileSettings
import rocks.fastpotify.android.ui.live.LiveFastpotifyApp
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.theme.FastpotifyTheme

class LiveUiStressTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun repeatedSpotifyItemsRemainStableWhileScrolling() {
        compose.activity.setContent {
            FastpotifyTheme {
                LiveFastpotifyApp(
                    FakeController(stressSnapshot()),
                    UiProfile.Phone,
                    {},
                    {},
                    requestNotificationPermission = false,
                )
            }
        }

        repeat(8) {
            compose.onNodeWithTag("home-list").performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Рекомендуем для вас").assertIsDisplayed()
    }

    @Test
    fun duplicatePlaylistAndQueueEntriesDoNotCrashLazyLists() {
        compose.activity.setContent {
            FastpotifyTheme {
                LiveFastpotifyApp(
                    FakeController(stressSnapshot()),
                    UiProfile.Phone,
                    {},
                    {},
                    requestNotificationPermission = false,
                )
            }
        }

        compose.onAllNodesWithText("Repeated playlist")[0].performClick()
        repeat(8) {
            compose.onNodeWithText("Repeated playlist").performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("mini-player").performClick()
        compose.onNodeWithContentDescription("Очередь").performClick()
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Repeated song").fetchSemanticsNodes().size > 1
        }
        repeat(8) {
            compose.onAllNodesWithText("Repeated song")[0].performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        assertTrue(compose.onAllNodesWithText("Repeated song").fetchSemanticsNodes().isNotEmpty())
    }

    private fun stressSnapshot(): LiveSnapshot {
        val track = LiveTrack(
            id = "same-id",
            uri = "spotify:track:same-id",
            title = "Repeated song",
            artist = "Test artist",
            album = "Test album",
            imageUrl = null,
            durationMs = 180_000,
            explicit = false,
        )
        val playlistCard = LiveCard(
            id = "playlist-id",
            uri = "spotify:playlist:playlist-id",
            title = "Repeated playlist",
            subtitle = "Test owner",
            imageUrl = null,
            kind = "playlist",
        )
        return LiveSnapshot(
            authState = AuthState.SignedIn,
            user = LiveUser("user", "Tester", null, "premium"),
            playlists = listOf(playlistCard),
            madeForYou = listOf(playlistCard),
            topArtists = List(20) { index ->
                LiveCard("artist-$index", "spotify:artist:$index", "Artist $index", "Исполнитель", null, "artist")
            },
            topTracks = List(40) { track },
            recentTracks = List(40) { track },
            recommendations = List(40) { track },
            openedPlaylist = LivePlaylist(
                id = playlistCard.id,
                uri = playlistCard.uri,
                title = playlistCard.title,
                description = "Repeated entries are valid in Spotify playlists",
                owner = "Test owner",
                imageUrl = null,
                total = 80,
                tracks = List(80) { track },
            ),
            nowPlaying = LiveNowPlaying(track, 1_000, true, false, "off", null),
            queue = List(40) { track },
            localPlayback = LocalPlaybackState.Connected,
        )
    }
}

private class FakeController(override val snapshot: LiveSnapshot) : LiveUiController {
    override fun signIn() = Unit
    override fun startLocalPlayback() = Unit
    override fun refresh() = Unit
    override fun search(query: String) = Unit
    override fun openPlaylist(id: String) = Unit
    override fun openContent(kind: String, id: String) = Unit
    override fun command(action: String, value: String) = Unit
    override fun play(track: LiveTrack) = Unit
    override fun playInContext(track: LiveTrack, contextUri: String) = Unit
    override fun playUri(uri: String) = Unit
    override fun queue(track: LiveTrack) = Unit
    override fun updateSettings(settings: MobileSettings) = Unit
    override fun createPlaylist(name: String) = Unit
    override fun addToPlaylist(playlistId: String, uri: String) = Unit
    override fun removeFromPlaylist(playlistId: String, uri: String) = Unit
    override fun signOut() = Unit
}
