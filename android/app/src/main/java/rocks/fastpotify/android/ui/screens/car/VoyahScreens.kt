package rocks.fastpotify.android.ui.screens.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.fastpotify.android.model.DemoSnapshot
import rocks.fastpotify.android.ui.components.AppBottomNavigation
import rocks.fastpotify.android.ui.components.AppScreen
import rocks.fastpotify.android.ui.components.CarNavigation
import rocks.fastpotify.android.ui.components.CoverArt
import rocks.fastpotify.android.ui.components.FeaturedCard
import rocks.fastpotify.android.ui.components.FilterBar
import rocks.fastpotify.android.ui.components.PlaybackControls
import rocks.fastpotify.android.ui.components.PlaybackProgress
import rocks.fastpotify.android.ui.components.PlayerUtilityActions
import rocks.fastpotify.android.ui.components.ProfileSelector
import rocks.fastpotify.android.ui.components.QuickMusicCard
import rocks.fastpotify.android.ui.components.TrackRow
import rocks.fastpotify.android.ui.profile.CarMetrics
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.theme.Accent
import rocks.fastpotify.android.ui.theme.Background
import rocks.fastpotify.android.ui.theme.Border
import rocks.fastpotify.android.ui.theme.Surface as AppSurface
import rocks.fastpotify.android.ui.theme.SurfaceRaised
import rocks.fastpotify.android.ui.theme.TextSecondary

@Composable
fun VoyahShell(
    snapshot: DemoSnapshot,
    selectedProfile: UiProfile,
    currentScreen: AppScreen,
    onProfileSelected: (UiProfile) -> Unit,
    onScreenSelected: (AppScreen) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .semantics { testTag = "screen-ready" },
    ) {
        VoyahSystemRail(
            selectedProfile = selectedProfile,
            currentScreen = currentScreen,
            onProfileSelected = onProfileSelected,
            onScreenSelected = onScreenSelected,
            onCheckForUpdates = onCheckForUpdates,
        )
        Column(Modifier.weight(1f)) {
            Row(Modifier.weight(1f)) {
                Box(Modifier.weight(1f)) {
                    when (currentScreen) {
                        AppScreen.Home -> VoyahHome(snapshot, onScreenSelected)
                        AppScreen.Playlist -> VoyahPlaylist(snapshot, onScreenSelected)
                        AppScreen.NowPlaying -> VoyahNowPlayingDetail(snapshot)
                        AppScreen.Search -> VoyahPlaceholder("Поиск", "Найдите музыку, артистов и подкасты", Icons.Default.Search)
                        AppScreen.Library -> VoyahPlaceholder("Моя медиатека", "Плейлисты, альбомы и любимые треки", Icons.Default.Home)
                        AppScreen.Create -> VoyahPlaceholder("Создать", "Создайте новый плейлист", Icons.Default.MoreHoriz)
                    }
                }
                PersistentNowPlayingPanel(snapshot)
            }
            AppBottomNavigation(
                items = CarNavigation,
                selected = currentScreen,
                onSelected = onScreenSelected,
                metrics = CarMetrics,
                modifier = Modifier.height(CarMetrics.navigationHeight),
            )
        }
    }
}

@Composable
private fun VoyahSystemRail(
    selectedProfile: UiProfile,
    currentScreen: AppScreen,
    onProfileSelected: (UiProfile) -> Unit,
    onScreenSelected: (AppScreen) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .fillMaxHeight()
            .background(Color(0xFF0D100E))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = Accent,
            onClick = { onScreenSelected(AppScreen.Home) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Home, "Главная", tint = Color.Black)
            }
        }
        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.weight(1f))
        Text("23°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("в салоне", color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(14.dp))
        ProfileSelector(
            profile = selectedProfile,
            onProfileSelected = onProfileSelected,
            onCheckForUpdates = onCheckForUpdates,
            modifier = Modifier.padding(horizontal = 6.dp),
            showLabel = false,
        )
    }
}

@Composable
private fun VoyahHome(snapshot: DemoSnapshot, onScreenSelected: (AppScreen) -> Unit) {
    var selectedFilter by remember { mutableStateOf(snapshot.filters.first()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CarMetrics.panelPadding),
        verticalArrangement = Arrangement.spacedBy(CarMetrics.spacing),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = Color(0xFFE7ECE8)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(snapshot.avatar, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.width(CarMetrics.spacing))
                FilterBar(snapshot.filters, selectedFilter) { selectedFilter = it }
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Same rule as Fastpotify desktop: roughly 300 dp per tile,
                // clamped to two through four columns as the center panel changes.
                val columns = (maxWidth.value / 300f).toInt().coerceIn(2, 4)
                val rows = (snapshot.quickCards.size + columns - 1) / columns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.height(
                        (CarMetrics.trackRowHeight * rows) +
                            (CarMetrics.smallSpacing * (rows - 1).coerceAtLeast(0)),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(CarMetrics.smallSpacing),
                    verticalArrangement = Arrangement.spacedBy(CarMetrics.smallSpacing),
                    userScrollEnabled = false,
                ) {
                    items(snapshot.quickCards, key = { it.id }) { item ->
                        QuickMusicCard(item, CarMetrics, onClick = { onScreenSelected(AppScreen.Playlist) })
                    }
                }
            }
        }
        item { SectionTitle("Специально для тебя") }
        item { FeaturedCard(snapshot.featured, CarMetrics, compact = false) }
        item { SectionTitle("Для тебя") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CarMetrics.spacing)) {
                items(snapshot.quickCards, key = { "for-${it.id}" }) { item ->
                    Column(modifier = Modifier.width(CarMetrics.cardWidth)) {
                        CoverArt(
                            palette = item.palette,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CarMetrics.cardWidth),
                            label = item.title,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoyahPlaylist(snapshot: DemoSnapshot, onScreenSelected: (AppScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CarMetrics.panelPadding),
        verticalArrangement = Arrangement.spacedBy(CarMetrics.smallSpacing),
    ) {
        item {
            IconButton(onClick = { onScreenSelected(AppScreen.Home) }, modifier = Modifier.size(CarMetrics.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CarMetrics.cardRadius))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF28483E), Color(0xFF151B18), AppSurface),
                        ),
                    )
                    .padding(CarMetrics.panelPadding),
                verticalAlignment = Alignment.Bottom,
            ) {
                CoverArt(snapshot.featured.palette, Modifier.size(180.dp), snapshot.featured.title)
                Spacer(Modifier.width(CarMetrics.spacing))
                Column(Modifier.weight(1f)) {
                    Text("ПЛЕЙЛИСТ", color = Accent, fontWeight = FontWeight.Bold)
                    Text(
                        snapshot.featured.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("3 трека • Fastpotify Demo", color = TextSecondary)
                }
                PlaylistActions()
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Для отдыха", "Природа", "Спокойствие").forEach { label ->
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }
        item {
            Surface(
                onClick = {},
                color = SurfaceRaised,
                shape = RoundedCornerShape(CarMetrics.cardRadius),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(CarMetrics.panelPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("＋", fontSize = 28.sp, color = Accent)
                    Spacer(Modifier.width(12.dp))
                    Text("Добавить в этот плейлист", fontWeight = FontWeight.Bold)
                }
            }
        }
        items(snapshot.tracks, key = { it.id }) { track ->
            TrackRow(track, CarMetrics)
        }
    }
}

@Composable
private fun PlaylistActions() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {}, modifier = Modifier.size(CarMetrics.touchTarget)) {
            Icon(Icons.Default.Download, "Скачать")
        }
        IconButton(onClick = {}, modifier = Modifier.size(CarMetrics.touchTarget)) {
            Icon(Icons.Default.Shuffle, "Перемешать")
        }
        Surface(
            onClick = {},
            color = Accent,
            shape = CircleShape,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Воспроизвести", tint = Color.Black, modifier = Modifier.size(42.dp))
            }
        }
    }
}

@Composable
private fun PersistentNowPlayingPanel(snapshot: DemoSnapshot) {
    Column(
        modifier = Modifier
            .width(CarMetrics.playerPanelWidth)
            .fillMaxHeight()
            .background(AppSurface)
            .padding(CarMetrics.panelPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сейчас играет", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, "Дополнительно") }
        }
        Spacer(Modifier.height(12.dp))
        CoverArt(
            palette = snapshot.nowPlaying.palette,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = snapshot.nowPlaying.title,
            radius = 24,
        )
        Spacer(Modifier.height(CarMetrics.spacing))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.nowPlaying.title, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                Text(snapshot.nowPlaying.artist, color = TextSecondary, fontSize = 17.sp)
            }
            PlayerUtilityActions(snapshot.nowPlaying, Modifier.width(142.dp))
        }
        Spacer(Modifier.height(14.dp))
        PlaybackProgress(snapshot.nowPlaying, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PlaybackControls(snapshot.nowPlaying, large = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VoyahNowPlayingDetail(snapshot: DemoSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CarMetrics.panelPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Текущая композиция", color = Accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            snapshot.nowPlaying.title,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(snapshot.nowPlaying.artist, color = TextSecondary, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Text(
            "Плеер остаётся доступным справа во время навигации. Здесь появятся очередь, текст и сведения о треке.",
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun VoyahPlaceholder(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Accent)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}
