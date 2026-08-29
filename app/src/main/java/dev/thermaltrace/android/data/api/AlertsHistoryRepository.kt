package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.AlertEventDto
import dev.thermaltrace.android.data.model.HistoryResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AlertsInboxRepository(
    private val api: ThermalTraceApi,
) {
    suspend fun listEvents(limit: Int = 30): Result<Pair<List<AlertEventDto>, Int>> =
        runCatching {
            val response = api.alertEvents(limit)
            if (response.code() == 401) error("Unauthorized — sign in again")
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
            }
            val body = response.body() ?: error("Empty alert events response")
            body.events to body.unackedCount
        }

    suspend fun acknowledge(eventId: Long): Result<Unit> = runCatching {
        val payload = JSONObject().put("event_id", eventId).toString()
            .toRequestBody("application/json".toMediaType())
        val response = api.ackAlertEvent(payload)
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (!response.isSuccessful) {
            val err = response.body()?.error
                ?: response.errorBody()?.string().orEmpty()
            error(err.ifBlank { "Ack failed (HTTP ${response.code()})" })
        }
    }

    suspend fun sendTest(): Result<String> = runCatching {
        val response = api.testAlert()
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (response.code() == 403) error("Editor role required to send a test")
        val body = response.body()
        if (!response.isSuccessful || body?.ok != true) {
            error(body?.error ?: "Test failed (HTTP ${response.code()})")
        }
        val sent = body.sent.joinToString(", ").ifBlank { "ok" }
        "Test sent via $sent"
    }
}

class HistoryRepository(
    private val api: ThermalTraceApi,
) {
    suspend fun load(days: Int = 7, probe: String? = null): Result<HistoryResponse> =
        runCatching {
            val response = api.history(days = days, probe = probe)
            if (response.code() == 401) error("Unauthorized — sign in again")
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
            }
            response.body() ?: error("Empty history response")
        }
}
