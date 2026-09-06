package rocks.fastpotify.android

import android.content.Context

object NativeBridge {
    init {
        System.loadLibrary("fastpotify_android")
    }

    @JvmStatic
    external fun contractVersion(): Int

    /** Debug instrumentation only: opens the same Android output used by playback. */
    @JvmStatic
    external fun audioProbe(): String

    @JvmStatic
    external fun demoSnapshotJson(profile: String, screen: String): String

    @JvmStatic
    external fun initialize(filesDir: String, context: Context)

    @JvmStatic
    external fun liveSnapshotJson(): String

    @JvmStatic
    external fun startSignIn(): String

    @JvmStatic
    external fun startLocalSignIn(): String

    @JvmStatic
    external fun refresh()

    @JvmStatic
    external fun search(query: String)

    @JvmStatic
    external fun openPlaylist(playlistId: String)

    @JvmStatic
    external fun openContent(kind: String, id: String)

    @JvmStatic
    external fun command(action: String, value: String = "")

    @JvmStatic
    external fun playTrack(trackJson: String)

    @JvmStatic
    external fun playContext(trackJson: String, contextUri: String)

    @JvmStatic
    external fun queueTrack(trackJson: String)

    @JvmStatic
    external fun updateSettings(json: String)

    @JvmStatic
    external fun createPlaylist(name: String)

    @JvmStatic
    external fun addToPlaylist(playlistId: String, uri: String)

    @JvmStatic
    external fun removeFromPlaylist(playlistId: String, uri: String)

    @JvmStatic
    external fun signOut()
}
