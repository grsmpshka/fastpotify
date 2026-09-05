package rocks.fastpotify.android

object NativeBridge {
    init {
        System.loadLibrary("fastpotify_android")
    }

    @JvmStatic
    external fun contractVersion(): Int

    @JvmStatic
    external fun demoSnapshotJson(profile: String, screen: String): String
}
