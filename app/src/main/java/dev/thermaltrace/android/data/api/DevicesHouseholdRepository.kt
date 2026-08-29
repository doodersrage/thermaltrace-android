package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.DeviceSummary
import dev.thermaltrace.android.data.model.HouseholdResponse
import dev.thermaltrace.android.data.model.LiveSensor

class DevicesRepository(
    private val api: ThermalTraceApi,
    private val formClient: SettingsFormClient,
) {
    suspend fun listDevices(): Result<List<DeviceSummary>> = runCatching {
        val response = api.homeReadings(save = 0)
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (!response.isSuccessful) {
            error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }
        val sensors = response.body()?.sensors.orEmpty()
        aggregateDevices(sensors)
    }

    suspend fun renameDevice(deviceId: String, name: String): Result<Unit> =
        formClient.renameDevice(deviceId, name)

    suspend fun setDeviceSpace(deviceId: String, space: String?): Result<Unit> =
        formClient.setDeviceSpace(deviceId, space)

    companion object {
        fun aggregateDevices(sensors: List<LiveSensor>): List<DeviceSummary> =
            sensors
                .groupBy { it.deviceId }
                .map { (id, rows) ->
                    DeviceSummary(
                        id = id,
                        name = rows.firstOrNull()?.deviceName?.ifBlank { "Device" } ?: "Device",
                        space = rows.firstOrNull()?.space,
                        sensorCount = rows.size,
                        kinds = rows.map { it.kind }.distinct().sorted(),
                    )
                }
                .sortedBy { it.name.lowercase() }
    }
}

class HouseholdRepository(
    private val api: ThermalTraceApi,
    private val formClient: SettingsFormClient,
) {
    suspend fun load(): Result<HouseholdResponse> = runCatching {
        val response = api.household()
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (!response.isSuccessful) {
            error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }
        response.body() ?: error("Empty household response")
    }

    suspend fun rename(name: String): Result<Unit> = formClient.renameHousehold(name)

    suspend fun invite(email: String, role: String): Result<Unit> =
        formClient.inviteHouseholdMember(email, role)

    suspend fun switchTo(householdId: String): Result<Unit> =
        formClient.switchHousehold(householdId)
}
