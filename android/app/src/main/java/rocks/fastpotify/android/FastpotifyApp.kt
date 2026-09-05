package rocks.fastpotify.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import rocks.fastpotify.android.model.DemoSnapshot
import rocks.fastpotify.android.ui.components.AppScreen
import rocks.fastpotify.android.ui.profile.ResolvedProfile
import rocks.fastpotify.android.ui.profile.UiProfile
import rocks.fastpotify.android.ui.profile.resolveProfile
import rocks.fastpotify.android.ui.screens.car.VoyahShell
import rocks.fastpotify.android.ui.screens.phone.PhoneShell
import rocks.fastpotify.android.ui.theme.Background
import rocks.fastpotify.android.ui.theme.FastpotifyTheme
import rocks.fastpotify.android.ui.theme.TextPrimary
import rocks.fastpotify.android.ui.theme.TextSecondary

@Composable
fun FastpotifyApp(initialProfile: String?, initialScreen: String?) {
    val context = LocalContext.current
    val profilePreferences = remember {
        context.getSharedPreferences("ui-profile", android.content.Context.MODE_PRIVATE)
    }
    val requestedProfile = remember(initialProfile) {
        parseProfile(initialProfile ?: profilePreferences.getString("selected", null))
    }
    val requestedScreen = remember(initialScreen) { parseScreen(initialScreen) }
    var profile by rememberSaveable { mutableStateOf(requestedProfile) }
    var screen by rememberSaveable { mutableStateOf(requestedScreen) }

    LaunchedEffect(profile, initialProfile) {
        if (initialProfile == null) {
            profilePreferences.edit().putString("selected", profile.wireValue).apply()
        }
    }

    val snapshotResult = remember {
        runCatching {
            check(NativeBridge.contractVersion() == 1) { "Unsupported Fastpotify core contract" }
            DemoSnapshot.fromJson(
                NativeBridge.demoSnapshotJson(
                    requestedProfile.wireValue,
                    requestedScreen.wireValue,
                ),
            )
        }
    }

    FastpotifyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background,
            contentColor = TextPrimary,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                val snapshot = snapshotResult.getOrNull()
                if (snapshot != null) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        when (resolveProfile(profile, maxWidth, maxHeight)) {
                            ResolvedProfile.VoyahFree -> VoyahShell(
                                snapshot = snapshot,
                                selectedProfile = profile,
                                currentScreen = screen,
                                onProfileSelected = { profile = it },
                                onScreenSelected = { screen = it },
                            )
                            ResolvedProfile.Phone -> PhoneShell(
                                snapshot = snapshot,
                                selectedProfile = profile,
                                currentScreen = screen,
                                onProfileSelected = { profile = it },
                                onScreenSelected = { screen = it },
                            )
                        }
                    }
                } else {
                    CoreUnavailable(snapshotResult.exceptionOrNull()?.message)
                }
            }
        }
    }
}

@Composable
private fun CoreUnavailable(message: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            color = Color(0xFF251717),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Fastpotify core недоступен", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(message ?: "Неизвестная ошибка", color = TextSecondary)
            }
        }
    }
}

private fun parseProfile(value: String?): UiProfile = when (value) {
    UiProfile.VoyahFree.wireValue -> UiProfile.VoyahFree
    UiProfile.Phone.wireValue -> UiProfile.Phone
    else -> UiProfile.Automatic
}

private fun parseScreen(value: String?): AppScreen = when (value) {
    AppScreen.Playlist.wireValue -> AppScreen.Playlist
    AppScreen.NowPlaying.wireValue -> AppScreen.NowPlaying
    AppScreen.Search.wireValue -> AppScreen.Search
    AppScreen.Library.wireValue -> AppScreen.Library
    AppScreen.Create.wireValue -> AppScreen.Create
    else -> AppScreen.Home
}
