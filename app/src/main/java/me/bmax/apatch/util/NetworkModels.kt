package me.bmax.apatch.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestVersionInfo(
    val versionCode: Int = 0,
    val downloadUrl: String = "",
    val changelog: String = ""
)

@Serializable
internal data class GithubReleaseDto(
    val body: String = "",
    val name: String = "",
    val assets: List<GithubAssetDto> = emptyList()
)

@Serializable
internal data class GithubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)