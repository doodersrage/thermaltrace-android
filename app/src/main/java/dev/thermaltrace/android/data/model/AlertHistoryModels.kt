package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertEventsResponse(
    val events: List<AlertEventDto> = emptyList(),
    @SerialName("unacked_count") val unackedCount: Int = 0,
    val error: String? = null,
)

@Serializable
data class AlertEventDto(
    val id: Long,
    @SerialName("user_id") val userId: String? = null,
    val kind: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("channels_sent") val channelsSent: List<String> = emptyList(),
    @SerialName("channels_skipped") val channelsSkipped: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
)

@Serializable
data class HistoryResponse(
    val days: Int = 7,
    val chart: HistoryChartDto? = null,
    val readings: HistoryReadingsDto? = null,
    val filters: HistoryFiltersDto? = null,
    val error: String? = null,
)

@Serializable
data class HistoryChartDto(
    val points: List<ChartPointDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class ChartPointDto(
    val timestamp: String,
    val tempf: Double,
    val humidity: Double = 0.0,
    val probeLabel: String = "",
)

@Serializable
data class HistoryReadingsDto(
    val readings: List<HistoryReadingDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null,
)

@Serializable
data class HistoryReadingDto(
    val tempc: Double = 0.0,
    val tempf: Double = 0.0,
    val humidity: Double = 0.0,
    val timestamp: String = "",
    @SerialName("feed_name") val feedName: String? = null,
    @SerialName("probe_label") val probeLabel: String? = null,
    @SerialName("probe_key") val probeKey: String? = null,
)

@Serializable
data class HistoryFiltersDto(
    val feeds: List<String> = emptyList(),
    val probes: List<HistoryProbeDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class HistoryProbeDto(
    val key: String,
    val label: String = "",
)

@Serializable
data class TestAlertResponse(
    val ok: Boolean = false,
    val sent: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val error: String? = null,
    val reason: String? = null,
)

@Serializable
data class AckResponse(
    val ok: Boolean = false,
    @SerialName("event_id") val eventId: Long? = null,
    val error: String? = null,
)
