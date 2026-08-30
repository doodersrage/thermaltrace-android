package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.AlertSettingsDto
import dev.thermaltrace.android.data.model.UserExportResponse
import dev.thermaltrace.android.data.model.UserPreferencesDto

class SettingsRepository(
    private val api: ThermalTraceApi,
    private val formClient: SettingsFormClient,
) {
    suspend fun loadExport(): Result<UserExportResponse> = runCatching {
        val response = api.userExport()
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (!response.isSuccessful) {
            error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }
        response.body() ?: error("Empty export response")
    }

    suspend fun savePreferences(prefs: UserPreferencesDto): Result<Unit> =
        formClient.savePreferences(prefs)

    suspend fun saveAlertSettings(settings: AlertSettingsDto): Result<Unit> =
        formClient.saveAlertSettings(settings)

    suspend fun snoozeHours(hours: Int): Result<Unit> =
        formClient.postSnoozeAction("snooze", hours = hours)

    suspend fun vacationDays(days: Int): Result<Unit> =
        formClient.postSnoozeAction("vacation", days = days)

    suspend fun snooze24(): Result<Unit> = snoozeHours(24)
    suspend fun vacation7(): Result<Unit> = vacationDays(7)
    suspend fun clearSnooze(): Result<Unit> = formClient.postSnoozeAction("clear_snooze")
    suspend fun clearVacation(): Result<Unit> = formClient.postSnoozeAction("clear_vacation")
}
