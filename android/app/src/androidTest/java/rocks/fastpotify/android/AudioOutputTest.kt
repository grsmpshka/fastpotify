package rocks.fastpotify.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioOutputTest {
    @Test
    fun nativePlayerCanOpenAndroidsDefaultOutput() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        NativeBridge.initialize(application.filesDir.absolutePath, application)
        val result = NativeBridge.audioProbe()
        assertTrue("Unexpected audio probe result: $result", result.contains("Hz on"))
    }
}
