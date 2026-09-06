package rocks.fastpotify.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.graphics.drawable.Icon
import android.graphics.Bitmap
import android.os.Looper
import rocks.fastpotify.android.model.LiveSnapshot
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the native librespot engine alive and exposes lock-screen controls. */
class PlaybackService : Service() {
    private lateinit var session: MediaSession
    private var focusRequest: AudioFocusRequest? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var imageLoader: ImageLoader
    private var artworkUrl: String? = null
    private var artwork: Bitmap? = null
    private var resumeOnFocusGain = false
    private val handler = Handler(Looper.getMainLooper())
    private val update = object : Runnable {
        override fun run() {
            runCatching { LiveSnapshot.fromJson(NativeBridge.liveSnapshotJson()) }
                .onSuccess(::publish)
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        imageLoader = ImageLoader(this)
        session = MediaSession(this, "Fastpotify").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = command("play")
                override fun onPause() = command("pause")
                override fun onSkipToNext() = command("next")
                override fun onSkipToPrevious() = command("previous")
                override fun onSeekTo(pos: Long) = command("seek", pos.toString())
                override fun onStop() { command("pause"); stopSelf() }
            })
            isActive = true
        }
        requestAudioFocus()
        startForeground(NOTIFICATION_ID, notification(LiveSnapshot()))
        handler.post(update)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "play", "pause", "next", "previous" -> command(intent.action!!)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(update)
        serviceScope.cancel()
        imageLoader.shutdown()
        session.release()
        focusRequest?.let { (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun command(action: String, value: String = "") = NativeBridge.command(action, value)

    private fun publish(snapshot: LiveSnapshot) {
        val now = snapshot.nowPlaying
        loadArtwork(now?.track?.imageUrl)
        val state = if (now?.playing == true) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO,
                )
                .setState(state, now?.positionMs ?: 0L, if (now?.playing == true) 1f else 0f)
                .build(),
        )
        session.setMetadata(
            android.media.MediaMetadata.Builder()
                .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, now?.track?.title ?: "Fastpotify")
                .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, now?.track?.artist ?: "Spotify Connect")
                .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, now?.track?.durationMs ?: 0L)
                .apply { artwork?.let { putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART, it) } }
                .build(),
        )
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(snapshot))
    }

    private fun notification(snapshot: LiveSnapshot): Notification {
        val now = snapshot.nowPlaying
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(now?.track?.title ?: "Fastpotify")
            .setContentText(now?.track?.artist ?: "Устройство Spotify Connect готово")
            .setContentIntent(open)
            .apply { artwork?.let(::setLargeIcon) }
            .setOngoing(now?.playing == true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .addAction(notificationAction(android.R.drawable.ic_media_previous, "Предыдущий", "previous", 1))
            .addAction(notificationAction(
                if (now?.playing == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (now?.playing == true) "Пауза" else "Играть",
                if (now?.playing == true) "pause" else "play",
                2,
            ))
            .addAction(notificationAction(android.R.drawable.ic_media_next, "Следующий", "next", 3))
            .build()
    }

    private fun action(command: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, PlaybackService::class.java).setAction(command),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notificationAction(icon: Int, title: String, command: String, requestCode: Int) =
        Notification.Action.Builder(Icon.createWithResource(this, icon), title, action(command, requestCode)).build()

    private fun requestAudioFocus() {
        val manager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { focus ->
                    when (focus) {
                        AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) {
                            resumeOnFocusGain = false
                            command("play")
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            resumeOnFocusGain = false
                            command("pause")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            resumeOnFocusGain = runCatching {
                                LiveSnapshot.fromJson(NativeBridge.liveSnapshotJson()).nowPlaying?.playing == true
                            }.getOrDefault(false)
                            command("pause")
                        }
                    }
                }
                .build()
        manager.requestAudioFocus(focusRequest!!)
    }

    private fun loadArtwork(url: String?) {
        if (url == artworkUrl) return
        artworkUrl = url
        artwork = null
        if (url == null) return
        serviceScope.launch {
            val request = ImageRequest.Builder(this@PlaybackService).data(url).allowHardware(false).build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult && artworkUrl == url) {
                artwork = result.drawable.toBitmap()
                handler.post {
                    runCatching { LiveSnapshot.fromJson(NativeBridge.liveSnapshotJson()) }.onSuccess(::publish)
                }
            }
        }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Воспроизведение", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        private const val CHANNEL_ID = "fastpotify_playback"
        private const val NOTIFICATION_ID = 42
        private const val UPDATE_INTERVAL_MS = 1000L
    }
}
