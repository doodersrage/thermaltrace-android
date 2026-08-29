package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserExportResponse(
    @SerialName("exported_at") val exportedAt: String? = null,
    val user: ExportUser? = null,
    val preferences: UserPreferencesDto? = null,
    @SerialName("alert_settings") val alertSettings: AlertSettingsDto? = null,
)

@Serializable
data class ExportUser(
    val id: String? = null,
    val email: String? = null,
)

@Serializable
data class UserPreferencesDto(
    val showGarageTemps: Boolean = true,
    val showWeather: Boolean = true,
    val weatherCityId: String? = null,
    val useCelsius: Boolean = false,
    val theme: String = "dark",
)

@Serializable
data class AlertSettingsDto(
    val enabled: Boolean = false,
    val digestEnabled: Boolean = false,
    val freezeThresholdF: Double = 34.0,
    val humidityThreshold: Double = 75.0,
    val rateChangeF: Double = 15.0,
    val outageHours: Double = 2.0,
    val email: String? = null,
    val channelEmail: Boolean = true,
    val channelSms: Boolean = false,
    val channelDiscord: Boolean = false,
    val channelPush: Boolean = false,
    val channelWebhook: Boolean = false,
    val channelTelegram: Boolean = false,
    val channelSlack: Boolean = false,
    val channelTeams: Boolean = false,
    val channelNtfy: Boolean = false,
    val channelPushover: Boolean = false,
    val channelWhatsapp: Boolean = false,
    val discordWebhookUrl: String? = null,
    val slackWebhookUrl: String? = null,
    val telegramBotToken: String? = null,
    val telegramChatId: String? = null,
    val forecastFreezeEnabled: Boolean = false,
    val nwsFreezeAlertsEnabled: Boolean = false,
    val forecastHoursAhead: Double = 12.0,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val quietHoursTimezone: String = "America/New_York",
    val quietHoursBypassFreeze: Boolean = true,
    val quietHoursSmsCritical: Boolean = false,
    val monthlyReportEnabled: Boolean = false,
    val quarterlyReportEnabled: Boolean = false,
    val dripEmailsEnabled: Boolean = false,
    val batteryAlertsEnabled: Boolean = false,
    val batteryTrendAlertsEnabled: Boolean = false,
    val batteryThresholdPct: Double = 20.0,
    val rssiAlertsEnabled: Boolean = false,
    val rssiThreshold: Double = -90.0,
    val escalationEnabled: Boolean = false,
    val escalationMinutes: Double = 60.0,
    val feedUptimeAlertsEnabled: Boolean = false,
    val portfolioAlertsEnabled: Boolean = false,
    val snoozeUntil: String? = null,
    val vacationUntil: String? = null,
    val ntfyTopic: String? = null,
    val ntfyServer: String = "https://ntfy.sh",
    val smsPhone: String? = null,
    val whatsappPhone: String? = null,
    val outboundWebhookUrl: String? = null,
    val readingWebhookUrl: String? = null,
)
