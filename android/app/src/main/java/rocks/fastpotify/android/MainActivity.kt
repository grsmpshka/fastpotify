package rocks.fastpotify.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestedProfile = if (BuildConfig.ENABLE_DEMO_MODE) {
            intent.getStringExtra(EXTRA_DEMO_PROFILE)
        } else {
            null
        }
        val requestedScreen = if (BuildConfig.ENABLE_DEMO_MODE) {
            intent.getStringExtra(EXTRA_DEMO_SCREEN)
        } else {
            null
        }
        if (BuildConfig.ENABLE_DEMO_MODE && requestedProfile != null) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        setContent {
            FastpotifyApp(
                initialProfile = requestedProfile,
                initialScreen = requestedScreen,
            )
        }
    }

    companion object {
        const val EXTRA_DEMO_PROFILE = "rocks.fastpotify.android.extra.DEMO_PROFILE"
        const val EXTRA_DEMO_SCREEN = "rocks.fastpotify.android.extra.DEMO_SCREEN"
    }
}
