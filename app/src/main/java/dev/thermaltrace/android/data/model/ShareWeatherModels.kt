package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val weather: WeatherSnapshotDto? = null,
    val cityId: String? = null,
    val error: String? = null,
)

@Serializable
data class WeatherSnapshotDto(
    val name: String? = null,
    val temp: Double = 0.0,
    val humidity: Double = 0.0,
    val description: String = "",
)

@Serializable
data class ShareLinksResponse(
    val links: List<ShareLinkDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class ShareLinkDto(
    val id: String,
    val token: String,
    val scope: String = "live",
    val label: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
