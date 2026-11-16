package com.rift.data.settings.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val size: Long = 0L,
    @SerialName("browser_download_url") val downloadUrl: String = "",
    @SerialName("content_type") val contentType: String = ""
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(
        val currentVersion: String,
        val newVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val releasePageUrl: String,
        val apkSize: Long
    ) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}
