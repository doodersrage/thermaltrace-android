package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeReadingsResponse(
    val updatedAt: String? = null,
    val groups: List<FeedGroup> = emptyList(),
    val sensors: List<LiveSensor> = emptyList(),
    val spaces: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class FeedGroup(
    val feedId: String? = null,
    val feedName: String? = null,
    val enabled: Boolean = true,
    val error: String? = null,
    val probes: List<ProbeReading> = emptyList(),
)

@Serializable
data class ProbeReading(
    val key: String,
    val label: String,
    val data: TempSample? = null,
)

@Serializable
data class TempSample(
    val f: Double,
    val c: Double,
    val h: Double = 0.0,
)

@Serializable
data class LiveSensor(
    val deviceId: String,
    val deviceName: String,
    val space: String? = null,
    val key: String,
    val label: String,
    val kind: String,
    val unit: String? = null,
    @SerialName("value_num") val valueNum: Double? = null,
    @SerialName("value_bool") val valueBool: Boolean? = null,
    @SerialName("value_text") val valueText: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
    val temp: TempSample? = null,
)
