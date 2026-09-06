package rocks.fastpotify.android.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import rocks.fastpotify.android.BuildConfig

private const val RELEASES_URL =
    "https://api.github.com/repos/grsmpshka/fastpotify/releases?per_page=20"
private const val APK_ASSET_NAME = "fastpotify-android.apk"
private const val UPDATE_DIRECTORY = "updates"
private val ANDROID_TAG = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)-android\\.(\\d+)$")

data class AndroidRelease(
    val versionCode: Int,
    val versionName: String,
    val pageUrl: String,
    val downloadUrl: String,
    val sha256: String,
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val release: AndroidRelease) : UpdateUiState
    data class Downloading(val release: AndroidRelease) : UpdateUiState
    data class Ready(val release: AndroidRelease, val apk: File) : UpdateUiState
    data object Current : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@Stable
class AndroidUpdater(private val context: Context) {
    private val preferences = context.getSharedPreferences("android-updates", Context.MODE_PRIVATE)

    var state: UpdateUiState by mutableStateOf(UpdateUiState.Idle)
        private set

    fun shouldCheckAutomatically(): Boolean {
        val lastCheck = preferences.getLong("last-successful-check", 0L)
        return System.currentTimeMillis() - lastCheck >= TimeUnit.DAYS.toMillis(1)
    }

    suspend fun check(manual: Boolean) {
        if (state is UpdateUiState.Checking || state is UpdateUiState.Downloading) return
        if (manual) state = UpdateUiState.Checking
        runCatching { withContext(Dispatchers.IO) { fetchNewestAndroidRelease() } }
            .onSuccess { release ->
                preferences.edit().putLong("last-successful-check", System.currentTimeMillis()).apply()
                state = when {
                    release != null && release.versionCode > BuildConfig.VERSION_CODE ->
                        UpdateUiState.Available(release)
                    manual -> UpdateUiState.Current
                    else -> UpdateUiState.Idle
                }
            }
            .onFailure { error ->
                state = if (manual) {
                    UpdateUiState.Error(error.message ?: "Не удалось проверить обновления")
                } else {
                    UpdateUiState.Idle
                }
            }
    }

    suspend fun download(release: AndroidRelease) {
        if (state is UpdateUiState.Downloading) return
        state = UpdateUiState.Downloading(release)
        runCatching { withContext(Dispatchers.IO) { downloadAndVerify(release) } }
            .onSuccess { apk -> state = UpdateUiState.Ready(release, apk) }
            .onFailure { error ->
                state = UpdateUiState.Error(error.message ?: "Не удалось скачать обновление")
            }
    }

    fun install(activity: Activity, ready: UpdateUiState.Ready) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            ready.apk,
        )
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun dismiss() {
        if (state !is UpdateUiState.Downloading) state = UpdateUiState.Idle
    }

    private fun fetchNewestAndroidRelease(): AndroidRelease? {
        val connection = openConnection(RELEASES_URL)
        check(connection.responseCode in 200..299) { "GitHub вернул ${connection.responseCode}" }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val releases = JSONArray(body)
        return buildList {
            for (index in 0 until releases.length()) {
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft", false)) continue
                val tag = release.optString("tag_name")
                val versionCode = versionCodeFromTag(tag) ?: continue
                val assets = release.getJSONArray("assets")
                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.getJSONObject(assetIndex)
                    if (asset.optString("name") != APK_ASSET_NAME) continue
                    val digest = asset.optString("digest").removePrefix("sha256:")
                    if (!digest.matches(Regex("[0-9a-fA-F]{64}"))) continue
                    add(
                        AndroidRelease(
                            versionCode = versionCode,
                            versionName = tag.removePrefix("v"),
                            pageUrl = release.getString("html_url"),
                            downloadUrl = asset.getString("browser_download_url"),
                            sha256 = digest.lowercase(),
                        ),
                    )
                }
            }
        }.maxByOrNull(AndroidRelease::versionCode)
    }

    private fun downloadAndVerify(release: AndroidRelease): File {
        val directory = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        val pending = File(directory, "fastpotify-android.apk.part")
        val destination = File(directory, APK_ASSET_NAME)
        pending.delete()

        val connection = openConnection(release.downloadUrl)
        check(connection.responseCode in 200..299) { "Загрузка вернула ${connection.responseCode}" }
        val digest = MessageDigest.getInstance("SHA-256")
        connection.inputStream.use { input ->
            pending.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
        }
        val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
        check(actualDigest == release.sha256) { "Контрольная сумма APK не совпала" }
        check(hasSameSigner(pending)) { "APK подписан другим сертификатом" }
        destination.delete()
        check(pending.renameTo(destination)) { "Не удалось подготовить APK к установке" }
        return destination
    }

    private fun hasSameSigner(apk: File): Boolean {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        return archive.packageName == context.packageName && signerDigests(installed) == signerDigests(archive)
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Fastpotify-Android/${BuildConfig.VERSION_NAME}")
        }
}

internal fun versionCodeFromTag(tag: String): Int? {
    val match = ANDROID_TAG.matchEntire(tag) ?: return null
    val (major, minor, patch, preview) = match.destructured.toList().map(String::toInt)
    if (major > 199 || minor > 99 || patch > 99 || preview > 999) return null
    return major * 10_000_000 + minor * 100_000 + patch * 1_000 + preview
}
