package rocks.fastpotify.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.fastpotify.android.model.FeaturedItem
import rocks.fastpotify.android.model.NowPlaying
import rocks.fastpotify.android.model.QuickCard
import rocks.fastpotify.android.model.Track
import rocks.fastpotify.android.ui.profile.UiMetrics
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.theme.Accent
import rocks.fastpotify.android.ui.theme.Border
import rocks.fastpotify.android.ui.theme.SurfaceRaised
import rocks.fastpotify.android.ui.theme.TextSecondary

enum class AppScreen(val wireValue: String) {
    Home("home"),
    Search("search"),
    Library("library"),
    Create("create"),
    Playlist("playlist"),
    NowPlaying("now_playing"),
}

data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
)

val CarNavigation = listOf(
    NavigationItem(AppScreen.Home, "Главная", Icons.Default.Home),
    NavigationItem(AppScreen.Search, "Поиск", Icons.Default.Search),
    NavigationItem(AppScreen.Library, "Моя медиатека", Icons.Default.LibraryMusic),
    NavigationItem(AppScreen.Create, "Создать", Icons.Default.AddCircle),
)

val PhoneNavigation = listOf(
    NavigationItem(AppScreen.Home, "Главная", Icons.Default.Home),
    NavigationItem(AppScreen.Search, "Поиск", Icons.Default.Search),
    NavigationItem(AppScreen.Library, "Медиатека", Icons.Default.LibraryMusic),
)

@Composable
fun CoverArt(
    palette: List<Long>,
    modifier: Modifier = Modifier,
    label: String = "F",
    radius: Int = 18,
) {
    val isLikedSongs = label.contains("любим", ignoreCase = true)
    val colors = if (isLikedSongs) {
        // Fastpotify's generated Liked Songs cover. It deliberately has no
        // network image because Spotify does not provide one for the collection.
        listOf(
            Color(0xFF450AF5),
            Color(0xFF6A3AE8),
            Color(0xFF8E9FE5),
            Color(0xFFC4EFD9),
        )
    } else {
        palette.map(::paletteColor)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
        )
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val artworkIcon = when {
                isLikedSongs -> Icons.Default.Favorite
                label.contains("radio", ignoreCase = true) -> Icons.Default.Radio
                label.contains("track", ignoreCase = true) -> Icons.Default.MusicNote
                else -> Icons.Default.AutoAwesome
            }
            Icon(
                imageVector = artworkIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(minOf(maxWidth, maxHeight) * 0.42f),
            )
        }
    }
}

@Composable
fun QuickMusicCard(
    item: QuickCard,
    metrics: UiMetrics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(metrics.trackRowHeight)
            .clip(RoundedCornerShape(metrics.cardRadius))
            .clickable(onClick = onClick),
        color = SurfaceRaised,
        shape = RoundedCornerShape(metrics.cardRadius),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverArt(
                palette = item.palette,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                label = item.title,
                radius = metrics.cardRadius.value.toInt(),
            )
            Column(
                modifier = Modifier.padding(horizontal = metrics.smallSpacing),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun FeaturedCard(
    item: FeaturedItem,
    metrics: UiMetrics,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SurfaceRaised,
        shape = RoundedCornerShape(metrics.cardRadius),
    ) {
        if (compact) {
            Column(Modifier.padding(metrics.panelPadding)) {
                CoverArt(
                    palette = item.palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f),
                    label = item.title,
                )
                Spacer(Modifier.height(metrics.spacing))
                FeaturedText(item)
                Spacer(Modifier.height(metrics.spacing))
                FeaturedActions(metrics)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(metrics.panelPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(
                    palette = item.palette,
                    modifier = Modifier.size(182.dp),
                    label = item.title,
                )
                Spacer(Modifier.width(metrics.spacing))
                Column(Modifier.weight(1f)) { FeaturedText(item) }
                Spacer(Modifier.width(metrics.spacing))
                FeaturedActions(metrics)
            }
        }
    }
}

@Composable
private fun FeaturedText(item: FeaturedItem) {
    Text(item.kind.uppercase(), color = Accent, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(
        item.title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(item.description, color = TextSecondary, maxLines = 3)
}

@Composable
private fun FeaturedActions(metrics: UiMetrics) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {},
            modifier = Modifier.size(metrics.touchTarget),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
        }
        Surface(
            modifier = Modifier.size(metrics.touchTarget),
            color = Accent,
            shape = CircleShape,
            onClick = {},
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Воспроизвести", tint = Color.Black)
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    metrics: UiMetrics,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.trackRowHeight)
            .clip(RoundedCornerShape(metrics.cardRadius))
            .clickable(onClick = {})
            .padding(horizontal = metrics.smallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            palette = track.palette,
            modifier = Modifier.size(metrics.trackRowHeight - 16.dp),
            label = track.title,
            radius = 10,
        )
        Spacer(Modifier.width(metrics.smallSpacing))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(track.artist, color = TextSecondary, maxLines = 1)
        }
        Text(track.metadata, color = TextSecondary)
        IconButton(onClick = {}, modifier = Modifier.size(metrics.touchTarget)) {
            Icon(Icons.Default.MoreVert, contentDescription = "Меню трека")
        }
    }
}

@Composable
fun PlaybackControls(
    nowPlaying: NowPlaying,
    large: Boolean,
    modifier: Modifier = Modifier,
) {
    val secondarySize = if (large) 56.dp else 44.dp
    val playSize = if (large) 72.dp else 52.dp
    val iconSize = if (large) 32.dp else 26.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {}, modifier = Modifier.size(secondarySize)) {
            Icon(Icons.Default.Shuffle, "Перемешать", tint = if (nowPlaying.shuffled) Accent else TextSecondary, modifier = Modifier.size(iconSize))
        }
        IconButton(onClick = {}, modifier = Modifier.size(secondarySize)) {
            Icon(Icons.Default.SkipPrevious, "Предыдущий", modifier = Modifier.size(iconSize))
        }
        Surface(
            modifier = Modifier.size(playSize),
            color = Color.White,
            shape = CircleShape,
            onClick = {},
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (nowPlaying.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (nowPlaying.playing) "Пауза" else "Воспроизвести",
                    tint = Color.Black,
                    modifier = Modifier.size(if (large) 40.dp else 30.dp),
                )
            }
        }
        IconButton(onClick = {}, modifier = Modifier.size(secondarySize)) {
            Icon(Icons.Default.SkipNext, "Следующий", modifier = Modifier.size(iconSize))
        }
        IconButton(onClick = {}, modifier = Modifier.size(secondarySize)) {
            Icon(Icons.Default.Repeat, "Повтор", tint = if (nowPlaying.repeating) Accent else TextSecondary, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun PlaybackProgress(nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
    val progress = (nowPlaying.positionMs.toFloat() / nowPlaying.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = Accent,
            trackColor = Border,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(nowPlaying.positionMs), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(nowPlaying.durationMs), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun PlayerUtilityActions(nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {}) {
            Icon(
                if (nowPlaying.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Любимый трек",
                tint = if (nowPlaying.liked) Accent else TextSecondary,
            )
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Devices, "Устройства") }
        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Очередь") }
    }
}

@Composable
fun ProfileSelector(
    profile: UiProfile,
    onProfileSelected: (UiProfile) -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            color = SurfaceRaised,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(14.dp)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(profile.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
                if (showLabel) {
                    Spacer(Modifier.width(8.dp))
                    Text(profile.label, fontWeight = FontWeight.Medium)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UiProfile.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { Icon(item.icon(), null) },
                    trailingIcon = {
                        if (item == profile) Icon(Icons.Default.Check, "Выбрано", tint = Accent)
                    },
                    onClick = {
                        onProfileSelected(item)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Проверить обновления") },
                leadingIcon = { Icon(Icons.Default.SystemUpdate, null) },
                onClick = {
                    expanded = false
                    onCheckForUpdates()
                },
            )
        }
    }
}

@Composable
fun FilterBar(filters: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        filters.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = Color.Black,
                ),
            )
        }
    }
}

@Composable
fun AppBottomNavigation(
    items: List<NavigationItem>,
    selected: AppScreen,
    onSelected: (AppScreen) -> Unit,
    metrics: UiMetrics,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, containerColor = SurfaceRaised) {
        items.forEach { item ->
            NavigationBarItem(
                selected = selected == item.screen,
                onClick = { onSelected(item.screen) },
                icon = { Icon(item.icon, item.label, modifier = Modifier.size(metrics.navigationIconSize)) },
                label = { Text(item.label, maxLines = 1, fontSize = metrics.navigationLabelSize) },
            )
        }
    }
}

@Composable
fun AppNavigationRail(selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    NavigationRail(containerColor = Color.Transparent) {
        CarNavigation.forEach { item ->
            NavigationRailItem(
                selected = selected == item.screen,
                onClick = { onSelected(item.screen) },
                icon = { Icon(item.icon, item.label) },
                label = null,
            )
        }
    }
}

private fun UiProfile.icon(): ImageVector = when (this) {
    UiProfile.Automatic -> Icons.Default.AutoAwesome
    UiProfile.VoyahFree -> Icons.Default.DirectionsCar
    UiProfile.Phone -> Icons.Default.Smartphone
}

private fun paletteColor(value: Long): Color = Color(0xFF000000 or value)

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
