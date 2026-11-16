package com.rift.data.settings.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Checks GitHub releases for a newer version of Rift.
     */
    suspend fun checkForUpdates(
        currentVersion: String,
        repo: String = "DeloNest/Rift"
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$repo/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "RiftLauncher-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 404) {
                return@withContext UpdateCheckResult.Error("لم يتم العثور على إصدارات منشورة في هذا المستودع بعد.")
            }
            if (responseCode != 200) {
                return@withContext UpdateCheckResult.Error("فشل الاتصال بخوادم GitHub (رمز الاستجابة: $responseCode)")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString<GitHubRelease>(body)

            val rawTag = release.tagName.removePrefix("v").trim()
            val cleanCurrent = currentVersion.removePrefix("v").trim()

            if (isNewerVersion(cleanCurrent, rawTag)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl

                UpdateCheckResult.UpdateAvailable(
                    currentVersion = currentVersion,
                    newVersion = release.tagName,
                    releaseName = release.name ?: release.tagName,
                    releaseNotes = release.body.orEmpty(),
                    downloadUrl = downloadUrl,
                    releasePageUrl = release.htmlUrl,
                    apkSize = apkAsset?.size ?: 0L
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "حدث خطأ أثناء فحص التحديثات")
        }
    }

    /**
     * Downloads the APK file and emits download progress percentages [0..100].
     */
    fun downloadApk(downloadUrl: String, targetFileName: String = "Rift-update.apk"): Flow<Int> = flow {
        withContext(Dispatchers.IO) {
            val destination = File(context.cacheDir, targetFileName)
            if (destination.exists()) {
                destination.delete()
            }

            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
            }

            // Follow redirect if needed (GitHub asset URLs usually redirect to AWS S3)
            var currentConn = conn
            var redirectCount = 0
            while (currentConn.responseCode in 300..399 && redirectCount < 5) {
                val newUrl = currentConn.getHeaderField("Location") ?: break
                currentConn.disconnect()
                currentConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                redirectCount++
            }

            val fileLength = currentConn.contentLength
            var totalBytesRead = 0L

            currentConn.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileLength > 0) {
                            val progress = ((totalBytesRead * 100) / fileLength).toInt()
                            emit(progress)
                        }
                    }
                }
            }
            emit(100)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Launches Android PackageInstaller Intent for the downloaded APK.
     */
    fun triggerInstall(targetFileName: String = "Rift-update.apk"): Boolean {
        return try {
            val file = File(context.cacheDir, targetFileName)
            if (!file.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * URI to open or add the repository directly into Obtainium.
     */
    fun getObtainiumUri(repo: String = "DeloNest/Rift"): Uri {
        return Uri.parse("obtainium://add/https://github.com/$repo")
    }

    /**
     * URL for the GitHub releases webpage.
     */
    fun getGitHubReleasesUrl(repo: String = "DeloNest/Rift"): Uri {
        return Uri.parse("https://github.com/$repo/releases")
    }

    /**
     * URL to download Obtainium from its official release repository.
     */
    fun getObtainiumDownloadUrl(): Uri {
        return Uri.parse("https://github.com/ImranR98/Obtainium/releases")
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.1" > "1.0.0").
     */
    private fun isNewerVersion(current: String, candidate: String): Boolean {
        val currParts = current.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val candParts = candidate.split('.', '-', '_').mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currParts.size, candParts.size)
        for (i in 0 until maxLen) {
            val currVal = currParts.getOrElse(i) { 0 }
            val candVal = candParts.getOrElse(i) { 0 }
            if (candVal > currVal) return true
            if (candVal < currVal) return false
        }
        return false
    }
}
