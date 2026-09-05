package rocks.fastpotify.android.model

import org.json.JSONObject

data class DemoSnapshot(
    val contractVersion: Int,
    val avatar: String,
    val filters: List<String>,
    val quickCards: List<QuickCard>,
    val featured: FeaturedItem,
    val tracks: List<Track>,
    val nowPlaying: NowPlaying,
) {
    companion object {
        fun fromJson(raw: String): DemoSnapshot {
            val json = JSONObject(raw)
            return DemoSnapshot(
                contractVersion = json.getInt("contract_version"),
                avatar = json.getString("avatar"),
                filters = json.getJSONArray("filters").strings(),
                quickCards = json.getJSONArray("quick_cards").objects { item ->
                    QuickCard(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        subtitle = item.getString("subtitle"),
                        palette = item.palette(),
                    )
                },
                featured = json.getJSONObject("featured").let { item ->
                    FeaturedItem(
                        title = item.getString("title"),
                        kind = item.getString("kind"),
                        description = item.getString("description"),
                        palette = item.palette(),
                    )
                },
                tracks = json.getJSONArray("tracks").objects { item ->
                    Track(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        metadata = item.getString("metadata"),
                        palette = item.palette(),
                    )
                },
                nowPlaying = json.getJSONObject("now_playing").let { item ->
                    NowPlaying(
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        positionMs = item.getLong("position_ms"),
                        durationMs = item.getLong("duration_ms"),
                        liked = item.getBoolean("liked"),
                        playing = item.getBoolean("playing"),
                        shuffled = item.getBoolean("shuffled"),
                        repeating = item.getBoolean("repeating"),
                        palette = item.palette(),
                    )
                },
            )
        }
    }
}

data class QuickCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val palette: List<Long>,
)

data class FeaturedItem(
    val title: String,
    val kind: String,
    val description: String,
    val palette: List<Long>,
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val metadata: String,
    val palette: List<Long>,
)

data class NowPlaying(
    val title: String,
    val artist: String,
    val positionMs: Long,
    val durationMs: Long,
    val liked: Boolean,
    val playing: Boolean,
    val shuffled: Boolean,
    val repeating: Boolean,
    val palette: List<Long>,
)

private fun org.json.JSONArray.strings(): List<String> =
    List(length()) { index -> getString(index) }

private fun <T> org.json.JSONArray.objects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

private fun JSONObject.palette(): List<Long> =
    getJSONArray("palette").let { values -> List(values.length()) { values.getLong(it) } }
