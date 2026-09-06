package rocks.fastpotify.android.model

import org.json.JSONArray
import org.json.JSONObject

enum class AuthState {
    SignedOut,
    SigningIn,
    SignedIn,
}

enum class LocalPlaybackState {
    SignedOut,
    SigningIn,
    Connecting,
    Connected,
}

data class LiveSnapshot(
    val revision: Long = 0,
    val authState: AuthState = AuthState.SignedOut,
    val busy: Boolean = false,
    val error: String? = null,
    val user: LiveUser? = null,
    val playlists: List<LiveCard> = emptyList(),
    val savedTracks: List<LiveTrack> = emptyList(),
    val libraryItems: List<LiveCard> = emptyList(),
    val madeForYou: List<LiveCard> = emptyList(),
    val topArtists: List<LiveCard> = emptyList(),
    val topTracks: List<LiveTrack> = emptyList(),
    val recommendations: List<LiveTrack> = emptyList(),
    val recentTracks: List<LiveTrack> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<LiveCard> = emptyList(),
    val openedPlaylist: LivePlaylist? = null,
    val nowPlaying: LiveNowPlaying? = null,
    val queue: List<LiveTrack> = emptyList(),
    val devices: List<LiveDevice> = emptyList(),
    val localPlayback: LocalPlaybackState = LocalPlaybackState.SignedOut,
    val localError: String? = null,
    val settings: MobileSettings = MobileSettings(),
) {
    companion object {
        fun fromJson(raw: String): LiveSnapshot {
            val json = JSONObject(raw)
            return LiveSnapshot(
                revision = json.optLong("revision"),
                authState = when (json.optString("auth_status")) {
                    "signed_in" -> AuthState.SignedIn
                    "signing_in" -> AuthState.SigningIn
                    else -> AuthState.SignedOut
                },
                busy = json.optBoolean("busy"),
                error = json.nullableString("error"),
                user = json.nullableObject("user")?.toUser(),
                playlists = json.array("playlists").mapObjects(JSONObject::toCard),
                savedTracks = json.array("saved_tracks").mapObjects(JSONObject::toTrack),
                libraryItems = json.array("library_items").mapObjects(JSONObject::toCard),
                madeForYou = json.array("made_for_you").mapObjects(JSONObject::toCard),
                topArtists = json.array("top_artists").mapObjects(JSONObject::toCard),
                topTracks = json.array("top_tracks").mapObjects(JSONObject::toTrack),
                recommendations = json.array("recommendations").mapObjects(JSONObject::toTrack),
                recentTracks = json.array("recent_tracks").mapObjects(JSONObject::toTrack),
                searchQuery = json.optString("search_query"),
                searchResults = json.array("search_results").mapObjects(JSONObject::toCard),
                openedPlaylist = json.nullableObject("opened_playlist")?.toPlaylist(),
                nowPlaying = json.nullableObject("now_playing")?.toNowPlaying(),
                queue = json.array("queue").mapObjects(JSONObject::toTrack),
                devices = json.array("devices").mapObjects(JSONObject::toDevice),
                localPlayback = when (json.optString("local_playback")) {
                    "connected" -> LocalPlaybackState.Connected
                    "connecting" -> LocalPlaybackState.Connecting
                    "signing_in" -> LocalPlaybackState.SigningIn
                    else -> LocalPlaybackState.SignedOut
                },
                localError = json.nullableString("local_error"),
                settings = json.optJSONObject("settings")?.toSettings() ?: MobileSettings(),
            )
        }
    }
}

data class LiveUser(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val product: String?,
)

data class MobileSettings(
    val bitrateKbps: Int = 320,
    val normalisation: Boolean = true,
    val autoplay: Boolean = true,
    val gapless: Boolean = true,
    val cacheMb: Int = 1024,
) {
    fun toJson(): String = JSONObject()
        .put("bitrate_kbps", bitrateKbps)
        .put("normalisation", normalisation)
        .put("autoplay", autoplay)
        .put("gapless", gapless)
        .put("cache_mb", cacheMb)
        .toString()
}

data class LiveCard(
    val id: String,
    val uri: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val kind: String,
)

data class LiveTrack(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String?,
    val durationMs: Long,
    val explicit: Boolean,
) {
    fun toJson(): String = JSONObject()
        .put("id", id).put("uri", uri).put("title", title).put("artist", artist)
        .put("album", album).put("image_url", imageUrl).put("duration_ms", durationMs)
        .put("explicit", explicit).toString()
}

data class LivePlaylist(
    val id: String,
    val uri: String,
    val title: String,
    val description: String,
    val owner: String,
    val imageUrl: String?,
    val total: Int,
    val tracks: List<LiveTrack>,
)

data class LiveNowPlaying(
    val track: LiveTrack,
    val positionMs: Long,
    val playing: Boolean,
    val shuffled: Boolean,
    val repeat: String,
    val deviceId: String?,
)

data class LiveDevice(
    val id: String?,
    val name: String,
    val kind: String,
    val active: Boolean,
    val restricted: Boolean,
    val volumePercent: Int?,
)

private fun JSONObject.toUser() = LiveUser(
    id = getString("id"),
    name = getString("name"),
    imageUrl = nullableString("image_url"),
    product = nullableString("product"),
)

private fun JSONObject.toCard() = LiveCard(
    id = getString("id"),
    uri = getString("uri"),
    title = getString("title"),
    subtitle = getString("subtitle"),
    imageUrl = nullableString("image_url"),
    kind = getString("kind"),
)

private fun JSONObject.toTrack() = LiveTrack(
    id = getString("id"),
    uri = getString("uri"),
    title = getString("title"),
    artist = getString("artist"),
    album = getString("album"),
    imageUrl = nullableString("image_url"),
    durationMs = getLong("duration_ms"),
    explicit = optBoolean("explicit"),
)

private fun JSONObject.toPlaylist() = LivePlaylist(
    id = getString("id"),
    uri = getString("uri"),
    title = getString("title"),
    description = getString("description"),
    owner = getString("owner"),
    imageUrl = nullableString("image_url"),
    total = getInt("total"),
    tracks = array("tracks").mapObjects(JSONObject::toTrack),
)

private fun JSONObject.toNowPlaying() = LiveNowPlaying(
    track = getJSONObject("track").toTrack(),
    positionMs = getLong("position_ms"),
    playing = getBoolean("playing"),
    shuffled = getBoolean("shuffled"),
    repeat = getString("repeat"),
    deviceId = nullableString("device_id"),
)

private fun JSONObject.toDevice() = LiveDevice(
    id = nullableString("id"),
    name = getString("name"),
    kind = getString("kind"),
    active = getBoolean("active"),
    restricted = getBoolean("restricted"),
    volumePercent = if (isNull("volume_percent")) null else getInt("volume_percent"),
)

private fun JSONObject.toSettings() = MobileSettings(
    bitrateKbps = optInt("bitrate_kbps", 320),
    normalisation = optBoolean("normalisation", true),
    autoplay = optBoolean("autoplay", true),
    gapless = optBoolean("gapless", true),
    cacheMb = optInt("cache_mb", 1024),
)

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf(String::isNotEmpty)

private fun JSONObject.nullableObject(name: String): JSONObject? =
    if (isNull(name)) null else optJSONObject(name)

private fun JSONObject.array(name: String): JSONArray = optJSONArray(name) ?: JSONArray()

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }
