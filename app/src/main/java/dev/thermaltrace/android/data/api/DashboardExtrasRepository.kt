package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.ClaimsEmailRequest
import dev.thermaltrace.android.data.model.ClaimsEmailResponse
import dev.thermaltrace.android.data.model.DoorEventsResponse
import dev.thermaltrace.android.data.model.HomeInsightsResponse
import dev.thermaltrace.android.data.model.IngestStatusResponse
import dev.thermaltrace.android.data.model.PortfolioResponse
import dev.thermaltrace.android.data.model.ReferralResponse
import dev.thermaltrace.android.data.model.ThermostatStatusResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class DashboardExtrasRepository(
    private val api: ThermalTraceApi,
    private val json: Json,
) {
    suspend fun loadPortfolio(): Result<PortfolioResponse> = runCatching {
        val response = api.portfolio()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Portfolio unavailable")
        response.body() ?: error("Empty portfolio response")
    }

    suspend fun loadHomeInsights(): Result<HomeInsightsResponse> = runCatching {
        val response = api.homeInsights()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Insights unavailable")
        response.body() ?: error("Empty insights response")
    }

    suspend fun loadDoorEvents(): Result<DoorEventsResponse> = runCatching {
        val response = api.doorEvents()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Door events unavailable")
        response.body() ?: error("Empty door events response")
    }

    suspend fun loadThermostatStatus(): Result<ThermostatStatusResponse> = runCatching {
        val response = api.thermostatStatus()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Thermostat unavailable")
        response.body() ?: error("Empty thermostat response")
    }

    suspend fun loadReferral(): Result<ReferralResponse> = runCatching {
        val response = api.referral()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Referral unavailable")
        response.body() ?: error("Empty referral response")
    }

    suspend fun loadIngestStatus(): Result<IngestStatusResponse> = runCatching {
        val response = api.ingestStatus()
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Ingest status unavailable")
        response.body() ?: error("Empty ingest status response")
    }

    suspend fun emailClaimsPack(
        adjusterEmail: String,
        from: String? = null,
        to: String? = null,
    ): Result<ClaimsEmailResponse> = runCatching {
        val response = api.emailClaimsPack(
            ClaimsEmailRequest(
                adjusterEmail = adjusterEmail,
                from = from,
                to = to,
            ),
        )
        val body = response.body()
        if (!response.isSuccessful || body?.ok != true) {
            error(body?.error ?: response.errorBody()?.string() ?: "Email failed")
        }
        body
    }

    suspend fun snoozeJson(action: String, hours: Int? = null, days: Int? = null): Result<Unit> =
        runCatching {
            val payload = buildJsonObject {
                put("action", action)
                hours?.let { put("hours", it) }
                days?.let { put("days", it) }
            }.toString()
            val body = payload.toRequestBody("application/json".toMediaType())
            val response = api.alertSnoozeJson(body)
            if (!response.isSuccessful || response.body()?.ok != true) {
                error(response.body()?.error ?: "Snooze failed")
            }
        }
}
