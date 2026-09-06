package rocks.fastpotify.android

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun captureAdaptiveScreens() {
        device.executeShellCommand("settings put global window_animation_scale 0")
        device.executeShellCommand("settings put global transition_animation_scale 0")
        device.executeShellCommand("settings put global animator_duration_scale 0")
        device.executeShellCommand("settings put system accelerometer_rotation 0")
        device.executeShellCommand("settings put system font_scale 1.0")
        device.executeShellCommand("settings put secure immersive_mode_confirmations confirmed")
        device.executeShellCommand("mkdir -p $SCREENSHOT_DIRECTORY")

        captureVoyah("home", "voyah-home.png")
        captureVoyah("playlist", "voyah-playlist.png")
        captureVoyah("now_playing", "voyah-now-playing.png")
        capturePhone("home", "phone-home.png")
        capturePhone("now_playing", "phone-now-playing.png")
    }

    private fun captureVoyah(screen: String, filename: String) {
        configureDisplay("1920x720", "160", rotation = 0)
        capture("voyah_free", screen, filename, 1920, 720)
    }

    private fun capturePhone(screen: String, filename: String) {
        configureDisplay("1440x3120", "420", rotation = 0)
        capture("phone", screen, filename, 1440, 3120)
    }

    private fun configureDisplay(size: String, density: String, rotation: Int) {
        device.executeShellCommand("wm size $size")
        device.executeShellCommand("wm density $density")
        device.executeShellCommand("settings put system user_rotation $rotation")
        device.waitForIdle()
    }

    private fun capture(
        profile: String,
        screen: String,
        filename: String,
        expectedWidth: Int,
        expectedHeight: Int,
    ) {
        val context = instrumentation.targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(MainActivity.EXTRA_DEMO_PROFILE, profile)
            putExtra(MainActivity.EXTRA_DEMO_SCREEN, screen)
        }

        ActivityScenario.launch<MainActivity>(intent).use {
            assertTrue(
                "Fastpotify UI did not become ready",
                // A cold CI emulator may still be finishing native library loading after
                // the display profile changes. Keep the readiness assertion bounded, but
                // allow the same startup budget as a real low-end Android device.
                device.wait(Until.hasObject(By.textContains("Test Track")), 30_000),
            )
            device.waitForIdle()
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            assertEquals("Unexpected screenshot width", expectedWidth, bitmap.width)
            assertEquals("Unexpected screenshot height", expectedHeight, bitmap.height)
            val output = "$SCREENSHOT_DIRECTORY/$filename"
            device.executeShellCommand("screencap -p $output")
            val fileSize = device.executeShellCommand("stat -c %s $output").trim().toLong()
            assertTrue("Screenshot is empty", fileSize > 10_000)
        }
    }

    companion object {
        private const val SCREENSHOT_DIRECTORY = "/sdcard/Download/FastpotifyScreenshots"
    }
}
