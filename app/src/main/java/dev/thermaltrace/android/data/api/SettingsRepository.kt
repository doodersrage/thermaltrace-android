package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.AlertSettingsDto
import dev.thermaltrace.android.data.model.UserExportResponse
import dev.thermaltrace.android.data.model.UserPreferencesDto

class SettingsRepository(
    private val api: ThermalTraceApi,
    private val formClient: SettingsFormClient,
) {
    @Volatile
    private var cachedExport: UserExportResponse? = null

    @Volatile
    private var cachedAtMs: Long = 0L

    suspend fun loadExport(force: Boolean = false): Result<UserExportResponse> {
        val hit = cachedExport
        if (!force && hit != null && (System.currentTimeMillis() - cachedAtMs) < CACHE_TTL_MS) {
            return Result.success(hit)
        }
        return runCatching {
            val response = api.userExport()
            if (response.code() == 401) error("Unauthorized — sign in again")
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
            }
            response.body() ?: error("Empty export response")
        }.onSuccess { body ->
            cachedExport = body
            cachedAtMs = System.currentTimeMillis()
        }
    }

    fun invalidateCache() {
        cachedExport = null
        cachedAtMs = 0L
    }

    suspend fun savePreferences(prefs: UserPreferencesDto): Result<Unit> =
        formClient.savePreferences(prefs).also { if (it.isSuccess) invalidateCache() }

    suspend fun saveAlertSettings(settings: AlertSettingsDto): Result<Unit> =
        formClient.saveAlertSettings(settings).also { if (it.isSuccess) invalidateCache() }

    suspend fun snoozeHours(hours: Int): Result<Unit> =
        formClient.postSnoozeAction("snooze", hours = hours).also { if (it.isSuccess) invalidateCache() }

    suspend fun vacationDays(days: Int): Result<Unit> =
        formClient.postSnoozeAction("vacation", days = days).also { if (it.isSuccess) invalidateCache() }

    suspend fun snooze24(): Result<Unit> = snoozeHours(24)
    suspend fun vacation7(): Result<Unit> = vacationDays(7)
    suspend fun clearSnooze(): Result<Unit> =
        formClient.postSnoozeAction("clear_snooze").also { if (it.isSuccess) invalidateCache() }

    suspend fun clearVacation(): Result<Unit> =
        formClient.postSnoozeAction("clear_vacation").also { if (it.isSuccess) invalidateCache() }

    companion object {
        private const val CACHE_TTL_MS = 60_000L
    }
}
