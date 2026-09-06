package rocks.fastpotify.android

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rocks.fastpotify.android.model.LiveSnapshot
import rocks.fastpotify.android.model.LocalPlaybackState
import rocks.fastpotify.android.model.MobileSettings
import rocks.fastpotify.android.model.LiveTrack

class LiveViewModel(application: Application) : AndroidViewModel(application) {
    var snapshot by mutableStateOf(LiveSnapshot())
        private set

    private var initialized = false
    private var serviceRunning = false
    @Volatile private var pendingUri: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                NativeBridge.initialize(application.filesDir.absolutePath, application)
                initialized = true
            }
            while (isActive) {
                if (initialized) {
                    runCatching { LiveSnapshot.fromJson(NativeBridge.liveSnapshotJson()) }
                        .onSuccess { next ->
                            updatePlaybackService(next)
                            if (next.authState == rocks.fastpotify.android.model.AuthState.SignedIn) {
                                pendingUri?.let { uri ->
                                    spotifyUri(uri)?.let { NativeBridge.command("play_uri", it) }
                                    pendingUri = null
                                }
                            }
                            withContext(Dispatchers.Main.immediate) { snapshot = next }
                        }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun signIn() {
        openAuthorization { NativeBridge.startSignIn() }
    }

    fun startLocalPlayback() {
        openAuthorization { NativeBridge.startLocalSignIn() }
    }

    private fun openAuthorization(urlProvider: () -> String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(urlProvider).onSuccess { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(intent)
            }
        }
    }

    fun refresh() = NativeBridge.refresh()

    fun search(query: String) = NativeBridge.search(query.trim())

    fun openPlaylist(id: String) = NativeBridge.openPlaylist(id)

    fun openContent(kind: String, id: String) = NativeBridge.openContent(kind, id)

    fun command(action: String, value: String = "") = NativeBridge.command(action, value)

    fun play(track: LiveTrack) = NativeBridge.playTrack(track.toJson())

    fun queue(track: LiveTrack) = NativeBridge.queueTrack(track.toJson())

    fun updateSettings(settings: MobileSettings) = NativeBridge.updateSettings(settings.toJson())

    fun createPlaylist(name: String) = NativeBridge.createPlaylist(name.trim())

    fun addToPlaylist(playlistId: String, uri: String) = NativeBridge.addToPlaylist(playlistId, uri)

    fun removeFromPlaylist(playlistId: String, uri: String) = NativeBridge.removeFromPlaylist(playlistId, uri)

    fun openExternalUri(uri: String) { pendingUri = uri }

    private fun spotifyUri(value: String): String? {
        if (value.startsWith("spotify:")) return value
        val uri = Uri.parse(value)
        if (uri.host != "open.spotify.com") return null
        val parts = uri.pathSegments
        val index = parts.indexOfFirst { it in setOf("track", "album", "artist", "playlist", "show", "episode") }
        return if (index >= 0 && index + 1 < parts.size) "spotify:${parts[index]}:${parts[index + 1]}" else null
    }

    fun signOut() = NativeBridge.signOut()

    private fun updatePlaybackService(next: LiveSnapshot) {
        val shouldRun = next.localPlayback == LocalPlaybackState.Connected
        if (shouldRun == serviceRunning) return
        serviceRunning = shouldRun
        val context = getApplication<Application>()
        if (shouldRun) {
            context.startForegroundService(Intent(context, PlaybackService::class.java))
        } else {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
    }
}
