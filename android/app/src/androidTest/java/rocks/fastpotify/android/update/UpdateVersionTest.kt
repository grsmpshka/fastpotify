package rocks.fastpotify.android.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateVersionTest {
    @Test
    fun androidPreviewTagsProduceMonotonicVersionCodes() {
        assertEquals(600001, versionCodeFromTag("v0.6.0-android.1"))
        assertEquals(600002, versionCodeFromTag("v0.6.0-android.2"))
        assertEquals(600006, versionCodeFromTag("v0.6.0-android.6"))
        assertEquals(701012, versionCodeFromTag("v0.7.1-android.12"))
        assertNull(versionCodeFromTag("v0.6.0"))
        assertNull(versionCodeFromTag("nightly"))
    }
}
