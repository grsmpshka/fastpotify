package rocks.fastpotify.android.ui.screens.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import rocks.fastpotify.android.ui.components.CoverArt
import rocks.fastpotify.android.ui.components.FeaturedCard
import rocks.fastpotify.android.ui.components.FilterBar
import rocks.fastpotify.android.ui.components.PhoneNavigation
import rocks.fastpotify.android.ui.components.PlaybackControls
import rocks.fastpotify.android.ui.components.PlaybackProgress
import rocks.fastpotify.android.ui.components.PlayerUtilityActions
import rocks.fastpotify.android.ui.components.ProfileSelector
import rocks.fastpotify.android.ui.components.TrackRow
import rocks.fastpotify.android.ui.profile.PhoneMetrics
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.theme.Accent
import rocks.fastpotify.android.ui.theme.Background
import rocks.fastpotify.android.ui.theme.SurfaceRaised
import rocks.fastpotify.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneShell(
    snapshot: DemoSnapshot,
    selectedProfile: UiProfile,
    currentScreen: AppScreen,
    onProfileSelected: (UiProfile) -> Unit,
    onScreenSelected: (AppScreen) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    if (currentScreen == AppScreen.NowPlaying) {
        PhoneNowPlaying(snapshot, onBack = { onScreenSelected(AppScreen.Home) })
        return
    }

    Scaffold(
        modifier = Modifier.semantics { testTag = "screen-ready" },
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle(currentScreen), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Surface(modifier = Modifier.padding(start = 12.dp).size(38.dp), shape = CircleShape, color = Color.White) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(snapshot.avatar, color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                },
                actions = {
                    ProfileSelector(
                        selectedProfile,
                        onProfileSelected,
                        onCheckForUpdates,
                        Modifier.padding(end = 8.dp),
                    )
                },
            )
        },
        bottomBar = {
            Column {
                MiniPlayer(snapshot, onClick = { onScreenSelected(AppScreen.NowPlaying) })
                AppBottomNavigation(
                    items = PhoneNavigation,
                    selected = currentScreen,
                    onSelected = onScreenSelected,
                    metrics = PhoneMetrics,
                    modifier = Modifier.height(PhoneMetrics.navigationHeight),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (currentScreen) {
                AppScreen.Home -> PhoneHome(snapshot, onScreenSelected)
                AppScreen.Playlist -> PhonePlaylist(snapshot, onScreenSelected)
                AppScreen.Search -> PhonePlaceholder("Поиск", "Музыка, артисты и подкасты", Icons.Default.Search)
                AppScreen.Library -> PhonePlaceholder("Медиатека", "Ваши плейлисты и альбомы", Icons.Default.LibraryMusic)
                AppScreen.Create -> PhonePlaceholder("Создать плейлист", "Добавляйте любимую музыку", Icons.Default.LibraryMusic)
                AppScreen.NowPlaying -> Unit
            }
        }
    }
}

@Composable
private fun PhoneHome(snapshot: DemoSnapshot, onScreenSelected: (AppScreen) -> Unit) {
    var selectedFilter by remember { mutableStateOf(snapshot.filters.first()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PhoneMetrics.panelPadding),
        verticalArrangement = Arrangement.spacedBy(PhoneMetrics.spacing),
    ) {
        item { FilterBar(snapshot.filters, selectedFilter) { selectedFilter = it } }
        item {
            Text("Добрый день", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        items(snapshot.quickCards.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PhoneMetrics.smallSpacing)) {
                row.forEach { item ->
                    rocks.fastpotify.android.ui.components.QuickMusicCard(
                        item = item,
                        metrics = PhoneMetrics,
                        modifier = Modifier.weight(1f),
                        onClick = { onScreenSelected(AppScreen.Playlist) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Text("Специально для тебя", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item { FeaturedCard(snapshot.featured, PhoneMetrics, compact = true) }
        item {
            Text("Для тебя", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhoneMetrics.spacing)) {
                items(snapshot.quickCards, key = { "phone-${it.id}" }) { item ->
                    Column(Modifier.width(PhoneMetrics.cardWidth)) {
                        CoverArt(item.palette, Modifier.fillMaxWidth().height(158.dp), item.title)
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhonePlaylist(snapshot: DemoSnapshot, onScreenSelected: (AppScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PhoneMetrics.panelPadding),
        verticalArrangement = Arrangement.spacedBy(PhoneMetrics.smallSpacing),
    ) {
        item {
            IconButton(onClick = { onScreenSelected(AppScreen.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
            }
        }
        item {
            CoverArt(snapshot.featured.palette, Modifier.fillMaxWidth().height(260.dp), snapshot.featured.title)
        }
        item {
            Text(snapshot.featured.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Плейлист • 3 трека", color = TextSecondary)
        }
        items(snapshot.tracks, key = { it.id }) { TrackRow(it, PhoneMetrics) }
    }
}

@Composable
private fun MiniPlayer(snapshot: DemoSnapshot, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(snapshot.nowPlaying.palette, Modifier.size(52.dp), snapshot.nowPlaying.title, 9)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(snapshot.nowPlaying.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(snapshot.nowPlaying.artist, color = TextSecondary, maxLines = 1)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Pause, "Пауза") }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Дополнительно") }
    }
}

@Composable
private fun PhoneNowPlaying(snapshot: DemoSnapshot, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(PhoneMetrics.panelPadding)
            .semantics { testTag = "screen-ready" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            Text("Сейчас играет", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Дополнительно") }
        }
        Spacer(Modifier.height(24.dp))
        CoverArt(
            snapshot.nowPlaying.palette,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            snapshot.nowPlaying.title,
            24,
        )
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.nowPlaying.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(snapshot.nowPlaying.artist, color = TextSecondary, fontSize = 18.sp)
            }
            PlayerUtilityActions(snapshot.nowPlaying, Modifier.width(150.dp))
        }
        Spacer(Modifier.height(20.dp))
        PlaybackProgress(snapshot.nowPlaying, Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        PlaybackControls(snapshot.nowPlaying, large = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun PhonePlaceholder(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(56.dp), tint = Accent)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary)
    }
}

private fun screenTitle(screen: AppScreen): String = when (screen) {
    AppScreen.Home -> "Fastpotify"
    AppScreen.Search -> "Поиск"
    AppScreen.Library -> "Медиатека"
    AppScreen.Create -> "Создать"
    AppScreen.Playlist -> "Плейлист"
    AppScreen.NowPlaying -> "Сейчас играет"
}
