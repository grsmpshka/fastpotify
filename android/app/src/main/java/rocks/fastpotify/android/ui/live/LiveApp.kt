package rocks.fastpotify.android.ui.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import rocks.fastpotify.android.LiveViewModel
import rocks.fastpotify.android.model.AuthState
import rocks.fastpotify.android.model.LiveCard
import rocks.fastpotify.android.model.LiveDevice
import rocks.fastpotify.android.model.LiveNowPlaying
import rocks.fastpotify.android.model.LocalPlaybackState
import rocks.fastpotify.android.model.MobileSettings
import rocks.fastpotify.android.model.LivePlaylist
import rocks.fastpotify.android.model.LiveSnapshot
import rocks.fastpotify.android.model.LiveTrack
import rocks.fastpotify.android.ui.components.ProfileSelector
import rocks.fastpotify.android.ui.profile.ResolvedProfile
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.profile.resolveProfile
import rocks.fastpotify.android.ui.theme.Accent
import rocks.fastpotify.android.ui.theme.Background
import rocks.fastpotify.android.ui.theme.Border
import rocks.fastpotify.android.ui.theme.SurfaceRaised
import rocks.fastpotify.android.ui.theme.TextSecondary

private enum class LiveScreen { Home, Search, Library, Playlist, NowPlaying }
private enum class Overlay { Queue, Devices, Settings, CreatePlaylist }

@Composable
fun LiveFastpotifyApp(
    viewModel: LiveViewModel,
    profile: UiProfile,
    onProfileSelected: (UiProfile) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val snapshot = viewModel.snapshot
    when (snapshot.authState) {
        AuthState.SignedOut, AuthState.SigningIn -> LoginScreen(snapshot, viewModel::signIn)
        AuthState.SignedIn -> BoxWithConstraints(Modifier.fillMaxSize()) {
            when (resolveProfile(profile, maxWidth, maxHeight)) {
                ResolvedProfile.VoyahFree -> CarShell(
                    snapshot,
                    viewModel,
                    profile,
                    onProfileSelected,
                    onCheckForUpdates,
                )
                ResolvedProfile.Phone -> PhoneShell(
                    snapshot,
                    viewModel,
                    profile,
                    onProfileSelected,
                    onCheckForUpdates,
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(snapshot: LiveSnapshot, signIn: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF163820), Background)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(Modifier.size(96.dp), color = Accent, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(58.dp), tint = Background)
                }
            }
            Text("Fastpotify", fontSize = 36.sp, fontWeight = FontWeight.Black)
            Text(
                "Войдите в Spotify. Пароль вводится только на странице Spotify.",
                color = TextSecondary,
            )
            Button(onClick = signIn, enabled = snapshot.authState != AuthState.SigningIn) {
                Icon(Icons.AutoMirrored.Filled.Login, null)
                Spacer(Modifier.width(8.dp))
                Text(if (snapshot.authState == AuthState.SigningIn) "Ожидаем Spotify…" else "Войти через Spotify")
            }
            if (snapshot.busy) CircularProgressIndicator(Modifier.size(28.dp))
            snapshot.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CarShell(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    profile: UiProfile,
    onProfileSelected: (UiProfile) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(LiveScreen.Home) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    Row(Modifier.fillMaxSize().background(Background).semantics { testTag = "live-screen-ready" }) {
        CarSidebar(
            snapshot = snapshot,
            selected = screen,
            onSelected = { screen = it },
            onPlaylist = {
                viewModel.openPlaylist(it.id)
                screen = LiveScreen.Playlist
            },
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            LiveTopBar(snapshot, profile, onProfileSelected, onCheckForUpdates, viewModel::refresh, { startLocalPlayback(context, viewModel) }, { overlay = Overlay.Settings }, { overlay = Overlay.CreatePlaylist }, viewModel::signOut, compact = false)
            Box(Modifier.weight(1f)) {
                ScreenContent(snapshot, screen, viewModel, { screen = it }, wide = true)
            }
        }
        Surface(Modifier.width(360.dp).fillMaxHeight(), color = SurfaceRaised) {
            NowPlayingPanel(snapshot, viewModel, onOverlay = { overlay = it }, large = false)
        }
    }
    OverlayContent(overlay, snapshot, viewModel) { overlay = null }
}

@Composable
private fun CarSidebar(
    snapshot: LiveSnapshot,
    selected: LiveScreen,
    onSelected: (LiveScreen) -> Unit,
    onPlaylist: (LiveCard) -> Unit,
) {
    Surface(Modifier.width(300.dp).fillMaxHeight(), color = androidx.compose.ui.graphics.Color(0xFF0E1110)) {
        Column(Modifier.padding(14.dp)) {
            NavigationRail(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                listOf(
                    Triple(LiveScreen.Home, "Главная", Icons.Default.Home),
                    Triple(LiveScreen.Search, "Поиск", Icons.Default.Search),
                    Triple(LiveScreen.Library, "Медиатека", Icons.Default.LibraryMusic),
                ).forEach { (screen, label, icon) ->
                    NavigationRailItem(
                        selected = selected == screen,
                        onClick = { onSelected(screen) },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                    )
                }
            }
            Text("Плейлисты", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(snapshot.playlists, key = { it.id }) { card ->
                    CompactCard(card, Modifier.fillMaxWidth()) { onPlaylist(card) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    profile: UiProfile,
    onProfileSelected: (UiProfile) -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(LiveScreen.Home) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    Scaffold(
        modifier = Modifier.semantics { testTag = "live-screen-ready" },
        containerColor = Background,
        topBar = {
            LiveTopBar(snapshot, profile, onProfileSelected, onCheckForUpdates, viewModel::refresh, { startLocalPlayback(context, viewModel) }, { overlay = Overlay.Settings }, { overlay = Overlay.CreatePlaylist }, viewModel::signOut, compact = true)
        },
        bottomBar = {
            Column {
                snapshot.nowPlaying?.let { now ->
                    MiniPlayer(now, viewModel) { screen = LiveScreen.NowPlaying }
                }
                NavigationBar(containerColor = SurfaceRaised) {
                    listOf(
                        Triple(LiveScreen.Home, "Главная", Icons.Default.Home),
                        Triple(LiveScreen.Search, "Поиск", Icons.Default.Search),
                        Triple(LiveScreen.Library, "Медиатека", Icons.Default.LibraryMusic),
                    ).forEach { (item, label, icon) ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (screen == LiveScreen.NowPlaying) {
                NowPlayingPanel(snapshot, viewModel, { overlay = it }, large = true) {
                    screen = LiveScreen.Home
                }
            } else {
                ScreenContent(snapshot, screen, viewModel, { screen = it }, wide = false)
            }
        }
    }
    OverlayContent(overlay, snapshot, viewModel) { overlay = null }
}

@Composable
private fun LiveTopBar(
    snapshot: LiveSnapshot,
    profile: UiProfile,
    onProfileSelected: (UiProfile) -> Unit,
    onCheckForUpdates: () -> Unit,
    onRefresh: () -> Unit,
    onLocalPlayback: () -> Unit,
    onSettings: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onSignOut: () -> Unit,
    compact: Boolean,
) {
    var more by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveArtwork(snapshot.user?.imageUrl, snapshot.user?.name.orEmpty(), Modifier.size(42.dp), true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(snapshot.user?.name ?: "Fastpotify", fontWeight = FontWeight.Bold)
            Text("Spotify ${snapshot.user?.product.orEmpty()}", color = TextSecondary, fontSize = 12.sp)
        }
        if (snapshot.busy) {
            if (compact) CircularProgressIndicator(Modifier.size(22.dp)) else LinearProgressIndicator(Modifier.width(80.dp))
        }
        IconButton(onClick = onLocalPlayback) {
            Icon(
                Icons.Default.Headphones,
                "Локальное воспроизведение",
                tint = if (snapshot.localPlayback == LocalPlaybackState.Connected) Accent else TextSecondary,
            )
        }
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Обновить") }
        if (!compact) {
            IconButton(onClick = onCreatePlaylist) { Icon(Icons.Default.Add, "Создать плейлист") }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки") }
            IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Filled.Logout, "Выйти") }
        } else {
            Box {
                IconButton(onClick = { more = true }) { Icon(Icons.Default.MoreVert, "Ещё") }
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    DropdownMenuItem(text = { Text("Создать плейлист") }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { more = false; onCreatePlaylist() })
                    DropdownMenuItem(text = { Text("Настройки") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { more = false; onSettings() })
                    DropdownMenuItem(text = { Text("Выйти") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }, onClick = { more = false; onSignOut() })
                }
            }
        }
        ProfileSelector(profile, onProfileSelected, onCheckForUpdates, showLabel = false)
    }
}

@Composable
private fun ScreenContent(
    snapshot: LiveSnapshot,
    screen: LiveScreen,
    viewModel: LiveViewModel,
    navigate: (LiveScreen) -> Unit,
    wide: Boolean,
) {
    when (screen) {
        LiveScreen.Home -> HomeScreen(snapshot, viewModel, navigate, wide)
        LiveScreen.Search -> SearchScreen(snapshot, viewModel, navigate)
        LiveScreen.Library -> LibraryScreen(snapshot, viewModel, navigate)
        LiveScreen.Playlist -> PlaylistScreen(snapshot.openedPlaylist, snapshot.playlists, viewModel, navigate, wide)
        LiveScreen.NowPlaying -> Unit
    }
}

@Composable
private fun HomeScreen(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    navigate: (LiveScreen) -> Unit,
    wide: Boolean,
) {
    var filter by remember { mutableStateOf("Все") }
    val quickCards = when (filter) {
        "Подкасты" -> snapshot.libraryItems.filter { it.kind == "show" }
        "Музыка" -> snapshot.playlists
        else -> snapshot.playlists + snapshot.libraryItems.filter { it.kind == "show" }.take(2)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Добрый день", fontSize = if (wide) 34.sp else 28.sp, fontWeight = FontWeight.Black) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Все", "Музыка", "Подкасты").forEach { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filter != "Подкасты") {
                    item { LikedSongsMediaCard(Modifier.width(if (wide) 190.dp else 168.dp)) { navigate(LiveScreen.Library) } }
                }
                items(quickCards.take(8), key = { "quick-${it.kind}-${it.id}" }) { card ->
                    MediaCard(card, Modifier.width(if (wide) 190.dp else 168.dp)) {
                        viewModel.openContent(card.kind, card.id)
                        navigate(LiveScreen.Playlist)
                    }
                }
            }
        }
        snapshot.topTracks.firstOrNull()?.let { featured ->
            item { SectionTitle("Специально для тебя") }
            item { FeaturedTrack(featured, wide) { viewModel.play(featured) } }
        }
        if (snapshot.recentTracks.isNotEmpty()) {
            item { SectionTitle("Недавно прослушано") }
            items(snapshot.recentTracks, key = { "recent-${it.id}-${it.uri}" }) { track ->
                TrackRow(track, { viewModel.play(track) }, viewModel, snapshot.playlists)
            }
        }
        if (snapshot.topTracks.isNotEmpty()) {
            item { SectionTitle("Часто слушаете") }
            items(snapshot.topTracks, key = { "top-${it.id}-${it.uri}" }) { track ->
                TrackRow(track, { viewModel.play(track) }, viewModel, snapshot.playlists)
            }
        }
        snapshot.error?.let { error -> item { ErrorCard(error) } }
    }
}

@Composable
private fun FeaturedTrack(track: LiveTrack, wide: Boolean, play: () -> Unit) {
    Surface(color = SurfaceRaised, shape = RoundedCornerShape(12.dp)) {
        if (wide) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LiveArtwork(track.imageUrl, track.title, Modifier.size(170.dp))
                Spacer(Modifier.width(18.dp))
                FeaturedTrackText(track, play, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                LiveArtwork(track.imageUrl, track.title, Modifier.fillMaxWidth().aspectRatio(1.6f))
                Spacer(Modifier.height(12.dp))
                FeaturedTrackText(track, play, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FeaturedTrackText(track: LiveTrack, play: () -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text("ДЛЯ ВАС", color = Accent, fontWeight = FontWeight.Bold)
        Text(track.title, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 2)
        Text("${track.artist} · ${track.album}", color = TextSecondary, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        Button(onClick = play) { Icon(Icons.Default.PlayArrow, null); Text("Воспроизвести") }
    }
}

@Composable
private fun SearchScreen(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    navigate: (LiveScreen) -> Unit,
) {
    var query by remember(snapshot.searchQuery) { mutableStateOf(snapshot.searchQuery) }
    val keyboard = LocalSoftwareKeyboardController.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Что хотите послушать?") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search(query)
                    keyboard?.hide()
                }),
            )
        }
        items(snapshot.searchResults, key = { "search-${it.kind}-${it.id}" }) { card ->
            CompactCard(card, Modifier.fillMaxWidth()) {
                if (card.kind in setOf("playlist", "album", "artist", "show")) {
                    viewModel.openContent(card.kind, card.id)
                    navigate(LiveScreen.Playlist)
                } else {
                    viewModel.command("play_uri", card.uri)
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    navigate: (LiveScreen) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LikedSongsRow(Modifier.fillMaxWidth()) { }
            Spacer(Modifier.height(10.dp))
            SectionTitle("Любимые треки")
        }
        items(snapshot.savedTracks, key = { "saved-${it.id}" }) { track ->
            TrackRow(track, { viewModel.play(track) }, viewModel, snapshot.playlists)
        }
        item { SectionTitle("Плейлисты") }
        items(snapshot.playlists, key = { "library-${it.id}" }) { card ->
            CompactCard(card, Modifier.fillMaxWidth()) {
                viewModel.openPlaylist(card.id)
                navigate(LiveScreen.Playlist)
            }
        }
        if (snapshot.libraryItems.isNotEmpty()) {
            item { SectionTitle("Альбомы, исполнители и подкасты") }
            items(snapshot.libraryItems, key = { "library-${it.kind}-${it.id}" }) { card ->
                CompactCard(card, Modifier.fillMaxWidth()) {
                    if (card.kind == "episode") {
                        viewModel.command("play_uri", card.uri)
                    } else {
                        viewModel.openContent(card.kind, card.id)
                        navigate(LiveScreen.Playlist)
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsMediaCard(modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp)) {
        LikedSongsArtwork(Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(8.dp))
        Text("Любимые треки", fontWeight = FontWeight.Bold, maxLines = 1)
        Text("Ваша медиатека", color = TextSecondary, maxLines = 1)
    }
}

@Composable
private fun LikedSongsRow(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(92.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF450AF5), androidx.compose.ui.graphics.Color(0xFFC4EFD9))))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Favorite, null, Modifier.size(34.dp), tint = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text("Любимые треки", fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun LikedSongsArtwork(modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(
            Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF450AF5), androidx.compose.ui.graphics.Color(0xFFC4EFD9))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Favorite, null, Modifier.fillMaxSize(0.32f), tint = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun PlaylistScreen(
    playlist: LivePlaylist?,
    playlists: List<LiveCard>,
    viewModel: LiveViewModel,
    navigate: (LiveScreen) -> Unit,
    wide: Boolean,
) {
    if (playlist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            IconButton(onClick = { navigate(LiveScreen.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
            }
            if (wide) Row(verticalAlignment = Alignment.CenterVertically) {
                LiveArtwork(playlist.imageUrl, playlist.title, Modifier.size(180.dp))
                Spacer(Modifier.width(20.dp))
                CollectionText(playlist, viewModel, Modifier.weight(1f))
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LiveArtwork(playlist.imageUrl, playlist.title, Modifier.size(220.dp))
                    Spacer(Modifier.height(14.dp))
                    CollectionText(playlist, viewModel, Modifier.fillMaxWidth())
                }
            }
        }
        items(playlist.tracks, key = { "playlist-${it.id}-${it.uri}" }) { track ->
            TrackRow(track, { viewModel.play(track) }, viewModel, playlists, playlist.id)
        }
    }
}

@Composable
private fun CollectionText(playlist: LivePlaylist, viewModel: LiveViewModel, modifier: Modifier) {
    Column(modifier) {
        Text("КОЛЛЕКЦИЯ", color = Accent, fontWeight = FontWeight.Bold)
        Text(playlist.title, fontSize = 30.sp, fontWeight = FontWeight.Black)
        if (playlist.description.isNotBlank()) Text(playlist.description, color = TextSecondary, maxLines = 3)
        Text("${playlist.owner} · ${playlist.total}", color = TextSecondary)
        Button(onClick = { viewModel.command("play_uri", playlist.uri) }) {
            Icon(Icons.Default.PlayArrow, null)
            Text("Воспроизвести")
        }
    }
}

@Composable
private fun MediaCard(card: LiveCard, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp)) {
        LiveArtwork(card.imageUrl, card.title, Modifier.fillMaxWidth().aspectRatio(1f), circle = card.kind == "artist")
        Spacer(Modifier.height(8.dp))
        Text(card.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(card.subtitle, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompactCard(card: LiveCard, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(64.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveArtwork(card.imageUrl, card.title, Modifier.size(52.dp), circle = card.kind == "artist")
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TrackRow(
    track: LiveTrack,
    onClick: () -> Unit,
    viewModel: LiveViewModel? = null,
    playlists: List<LiveCard> = emptyList(),
    removableFrom: String? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveArtwork(track.imageUrl, track.title, Modifier.size(54.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.explicit) Text("E", color = TextSecondary, fontSize = 11.sp)
        Text(formatTime(track.durationMs), color = TextSecondary, modifier = Modifier.padding(horizontal = 10.dp))
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Меню") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (viewModel != null) {
                    DropdownMenuItem(
                        text = { Text("Добавить в очередь") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
                        onClick = { viewModel.queue(track); menu = false },
                    )
                    playlists.forEach { playlist ->
                        DropdownMenuItem(
                            text = { Text("В ${playlist.title}", maxLines = 1) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                            onClick = { viewModel.addToPlaylist(playlist.id, track.uri); menu = false },
                        )
                    }
                    if (removableFrom != null) {
                        DropdownMenuItem(
                            text = { Text("Удалить из плейлиста") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { viewModel.removeFromPlaylist(removableFrom, track.uri); menu = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(now: LiveNowPlaying, viewModel: LiveViewModel, open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(SurfaceRaised).clickable(onClick = open).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveArtwork(now.track.imageUrl, now.track.title, Modifier.size(54.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(now.track.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(now.track.artist, color = TextSecondary, maxLines = 1)
        }
        IconButton(onClick = { viewModel.command(if (now.playing) "pause" else "play") }) {
            Icon(if (now.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
        }
    }
}

@Composable
private fun NowPlayingPanel(
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    onOverlay: (Overlay) -> Unit,
    large: Boolean,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val now = snapshot.nowPlaying
    if (now == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Headphones, null, Modifier.size(52.dp), tint = TextSecondary)
            Spacer(Modifier.height(14.dp))
            Text("Ничего не играет", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                when (snapshot.localPlayback) {
                    LocalPlaybackState.Connected -> "Fastpotify готов как устройство Spotify Connect"
                    LocalPlaybackState.Connecting -> "Подключаем устройство Spotify Connect…"
                    LocalPlaybackState.SigningIn -> "Завершите вход в открывшемся окне Spotify"
                    LocalPlaybackState.SignedOut -> "Подключите этот телефон для воспроизведения через librespot"
                },
                color = TextSecondary,
            )
            if (snapshot.localPlayback == LocalPlaybackState.SignedOut) {
                Spacer(Modifier.height(18.dp))
                Button(onClick = { startLocalPlayback(context, viewModel) }) { Text("Включить на этом устройстве") }
            }
            snapshot.localError?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }
    Column(
        Modifier.fillMaxSize().padding(if (large) 20.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                Text("Сейчас играет", Modifier.weight(1f).align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
            }
        }
        LiveArtwork(
            now.track.imageUrl,
            now.track.title,
            Modifier.fillMaxWidth().then(if (large) Modifier.aspectRatio(1f) else Modifier.height(290.dp)),
        )
        Column(Modifier.fillMaxWidth()) {
            Text(now.track.title, fontSize = if (large) 26.sp else 21.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Text(now.track.artist, color = TextSecondary, fontSize = 16.sp, maxLines = 1)
        }
        PlaybackSlider(now) { viewModel.command("seek", it.toString()) }
        PlaybackButtons(now, viewModel)
        VolumeControl { viewModel.command("volume", it.toString()) }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            IconButton(onClick = { viewModel.command("save", now.track.uri) }) {
                Icon(Icons.Default.Favorite, "Добавить в медиатеку", tint = Accent)
            }
            IconButton(onClick = { onOverlay(Overlay.Devices) }) { Icon(Icons.Default.Devices, "Устройства") }
            IconButton(onClick = { onOverlay(Overlay.Queue) }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Очередь") }
        }
    }
}

@Composable
private fun VolumeControl(setVolume: (Int) -> Unit) {
    var volume by remember { mutableFloatStateOf(50f) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, "Громкость", tint = TextSecondary)
        Slider(
            value = volume,
            onValueChange = { volume = it },
            onValueChangeFinished = { setVolume(volume.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlaybackSlider(now: LiveNowPlaying, seek: (Long) -> Unit) {
    var value by remember(now.track.uri, now.positionMs) { mutableFloatStateOf(now.positionMs.toFloat()) }
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { seek(value.toLong()) },
            valueRange = 0f..now.track.durationMs.coerceAtLeast(1).toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(value.toLong()), color = TextSecondary, fontSize = 12.sp)
            Text(formatTime(now.track.durationMs), color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlaybackButtons(now: LiveNowPlaying, viewModel: LiveViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.command("shuffle", (!now.shuffled).toString()) }) {
            Icon(Icons.Default.Shuffle, "Перемешать", tint = if (now.shuffled) Accent else TextSecondary)
        }
        IconButton(onClick = { viewModel.command("previous") }) { Icon(Icons.Default.SkipPrevious, "Предыдущий", Modifier.size(32.dp)) }
        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = androidx.compose.ui.graphics.Color.White, onClick = {
            viewModel.command(if (now.playing) "pause" else "play")
        }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (now.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Background, modifier = Modifier.size(36.dp))
            }
        }
        IconButton(onClick = { viewModel.command("next") }) { Icon(Icons.Default.SkipNext, "Следующий", Modifier.size(32.dp)) }
        IconButton(onClick = {
            val next = when (now.repeat) { "off" -> "context"; "context" -> "track"; else -> "off" }
            viewModel.command("repeat", next)
        }) { Icon(Icons.Default.Repeat, "Повтор", tint = if (now.repeat != "off") Accent else TextSecondary) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayContent(
    overlay: Overlay?,
    snapshot: LiveSnapshot,
    viewModel: LiveViewModel,
    dismiss: () -> Unit,
) {
    when (overlay) {
        Overlay.Queue -> ModalBottomSheet(onDismissRequest = dismiss) {
            Text("Очередь", Modifier.padding(horizontal = 20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.fillMaxWidth().height(420.dp), contentPadding = PaddingValues(12.dp)) {
                items(snapshot.queue, key = { "queue-${it.id}-${it.uri}" }) { track ->
                    TrackRow(track, { viewModel.play(track) }, viewModel, snapshot.playlists)
                }
            }
        }
        Overlay.Devices -> AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {},
            title = { Text("Устройства Spotify Connect") },
            text = {
                LazyColumn {
                    items(snapshot.devices, key = { it.id ?: it.name }) { device ->
                        DeviceRow(device) {
                            device.id?.let { viewModel.command("transfer", it) }
                            dismiss()
                        }
                    }
                }
            },
        )
        Overlay.Settings -> PlaybackSettingsDialog(snapshot.settings, viewModel::updateSettings, dismiss)
        Overlay.CreatePlaylist -> CreatePlaylistDialog(viewModel::createPlaylist, dismiss)
        null -> Unit
    }
}

@Composable
private fun CreatePlaylistDialog(create: (String) -> Unit, dismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Новый плейлист") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true) },
        confirmButton = {
            Button(onClick = { create(name); dismiss() }, enabled = name.isNotBlank()) { Text("Создать") }
        },
    )
}

@Composable
private fun PlaybackSettingsDialog(
    initial: MobileSettings,
    save: (MobileSettings) -> Unit,
    dismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Настройки воспроизведения") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Качество", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(96, 160, 320).forEach { bitrate ->
                        Button(onClick = { value = value.copy(bitrateKbps = bitrate) }) {
                            Text(if (value.bitrateKbps == bitrate) "✓ $bitrate" else "$bitrate")
                        }
                    }
                }
                SettingSwitch("Нормализация громкости", value.normalisation) { value = value.copy(normalisation = it) }
                SettingSwitch("Автовоспроизведение", value.autoplay) { value = value.copy(autoplay = it) }
                SettingSwitch("Плавные переходы без пауз", value.gapless) { value = value.copy(gapless = it) }
                Text("Кэш аудио: ${value.cacheMb} МБ", fontWeight = FontWeight.Bold)
                Slider(
                    value = value.cacheMb.toFloat(),
                    onValueChange = { value = value.copy(cacheMb = (it / 256).toInt() * 256) },
                    valueRange = 0f..2048f,
                    steps = 7,
                )
                Text("Изменение параметров перезапустит локальный плеер.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = { save(value); dismiss() }) { Text("Сохранить") }
        },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DeviceRow(device: LiveDevice, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = !device.restricted, onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Devices, null, tint = if (device.active) Accent else TextSecondary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(device.name, fontWeight = FontWeight.Bold)
            Text(device.kind, color = TextSecondary)
        }
    }
}

@Composable
private fun LiveArtwork(
    imageUrl: String?,
    label: String,
    modifier: Modifier,
    circle: Boolean = false,
) {
    val shape = if (circle) CircleShape else RoundedCornerShape(8.dp)
    Box(
        modifier.clip(shape).background(
            Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF315C43), androidx.compose.ui.graphics.Color(0xFF17241B))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Accent.copy(alpha = 0.7f), modifier = Modifier.fillMaxSize(0.38f))
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 23.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun ErrorCard(error: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
        Text(error, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun startLocalPlayback(context: Context, viewModel: LiveViewModel) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        (context as? Activity)?.let {
            ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }
    viewModel.startLocalPlayback()
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
