package rocks.fastpotify.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF080A09)
val Surface = Color(0xFF111412)
val SurfaceRaised = Color(0xFF1A1E1B)
val Border = Color(0xFF292E2A)
val Accent = Color(0xFF63DB90)
val TextPrimary = Color(0xFFF4F7F4)
val TextSecondary = Color(0xFFABB4AD)

private val FastpotifyColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF062411),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = Border,
)

@Composable
fun FastpotifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FastpotifyColors,
        typography = Typography(),
        content = content,
    )
}
