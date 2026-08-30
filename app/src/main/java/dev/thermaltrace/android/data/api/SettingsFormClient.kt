package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.data.model.AlertSettingsDto
import dev.thermaltrace.android.data.model.UserPreferencesDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Dashboard settings writes are multipart/form POSTs that 302-redirect.
 * We disable follow-redirects and treat `*?…_saved=1` / snooze query flags as success.
 */
class SettingsFormClient(
    private val baseUrl: String,
    cookieJar: SessionCookieJar,
    logging: okhttp3.logging.HttpLoggingInterceptor,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(logging)
        .build()

    private val root = baseUrl.trimEnd('/')

    suspend fun savePreferences(prefs: UserPreferencesDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/settings")
                    .add("theme", prefs.theme.ifBlank { "dark" })
                    .apply {
                        if (prefs.showGarageTemps) add("show_garage_temps", "on")
                        if (prefs.showWeather) add("show_weather", "on")
                        if (prefs.useCelsius) add("use_celsius", "on")
                        prefs.weatherCityId?.takeIf { it.isNotBlank() }?.let {
                            add("weather_city_id", it)
                        }
                    }
                    .build()
                postForm("$root/api/user/preferences", body, successHints = listOf("prefs_saved=1"))
            }
        }

    suspend fun saveAlertSettings(settings: AlertSettingsDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/alerts")
                    .addCheckbox("alerts_enabled", settings.enabled)
                    .addCheckbox("digest_enabled", settings.digestEnabled)
                    .addCheckbox("monthly_report_enabled", settings.monthlyReportEnabled)
                    .addCheckbox("quarterly_report_enabled", settings.quarterlyReportEnabled)
                    .addCheckbox("drip_emails_enabled", settings.dripEmailsEnabled)
                    .addCheckbox("forecast_freeze_enabled", settings.forecastFreezeEnabled)
                    .addCheckbox("nws_freeze_alerts_enabled", settings.nwsFreezeAlertsEnabled)
                    .addCheckbox("quiet_hours_enabled", settings.quietHoursEnabled)
                    .addCheckbox("quiet_hours_bypass_freeze", settings.quietHoursBypassFreeze)
                    .addCheckbox("quiet_hours_sms_critical", settings.quietHoursSmsCritical)
                    .addCheckbox("battery_alerts_enabled", settings.batteryAlertsEnabled)
                    .addCheckbox("battery_trend_alerts_enabled", settings.batteryTrendAlertsEnabled)
                    .addCheckbox("rssi_alerts_enabled", settings.rssiAlertsEnabled)
                    .addCheckbox("escalation_enabled", settings.escalationEnabled)
                    .addCheckbox("feed_uptime_alerts_enabled", settings.feedUptimeAlertsEnabled)
                    .addCheckbox("portfolio_alerts_enabled", settings.portfolioAlertsEnabled)
                    .addCheckbox("channel_email", settings.channelEmail)
                    .addCheckbox("channel_sms", settings.channelSms)
                    .addCheckbox("channel_discord", settings.channelDiscord)
                    .addCheckbox("channel_push", settings.channelPush)
                    .addCheckbox("channel_webhook", settings.channelWebhook)
                    .addCheckbox("channel_telegram", settings.channelTelegram)
                    .addCheckbox("channel_slack", settings.channelSlack)
                    .addCheckbox("channel_teams", settings.channelTeams)
                    .addCheckbox("channel_ntfy", settings.channelNtfy)
                    .addCheckbox("channel_pushover", settings.channelPushover)
                    .addCheckbox("channel_whatsapp", settings.channelWhatsapp)
                    .add("freeze_threshold_f", settings.freezeThresholdF.toString())
                    .add("humidity_threshold", settings.humidityThreshold.toString())
                    .add("rate_change_f", settings.rateChangeF.toString())
                    .add("outage_hours", settings.outageHours.toString())
                    .add("forecast_hours_ahead", settings.forecastHoursAhead.toString())
                    .add("quiet_hours_start", settings.quietHoursStart)
                    .add("quiet_hours_end", settings.quietHoursEnd)
                    .add("quiet_hours_timezone", settings.quietHoursTimezone)
                    .add("battery_threshold_pct", settings.batteryThresholdPct.toString())
                    .add("rssi_threshold", settings.rssiThreshold.toString())
                    .add("escalation_minutes", settings.escalationMinutes.toString())
                    .addNullable("alert_email", settings.email)
                    .addNullable("discord_webhook_url", settings.discordWebhookUrl)
                    .addNullable("slack_webhook_url", settings.slackWebhookUrl)
                    .addNullable("telegram_bot_token", settings.telegramBotToken)
                    .addNullable("telegram_chat_id", settings.telegramChatId)
                    .addNullable("ntfy_topic", settings.ntfyTopic)
                    .add("ntfy_server", settings.ntfyServer)
                    .addNullable("sms_phone", settings.smsPhone)
                    .addNullable("whatsapp_phone", settings.whatsappPhone)
                    .addNullable("outbound_webhook_url", settings.outboundWebhookUrl)
                    .addNullable("reading_webhook_url", settings.readingWebhookUrl)
                    .build()
                postForm(
                    "$root/api/user/alert-settings",
                    body,
                    successHints = listOf("alert_saved=1"),
                )
            }
        }

    suspend fun postSnoozeAction(
        action: String,
        hours: Int? = null,
        days: Int? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/alerts")
                    .add("action", action)
                    .apply {
                        if (hours != null) add("hours", hours.toString())
                        if (days != null) add("days", days.toString())
                    }
                    .build()
                postForm(
                    "$root/api/user/alert-snooze",
                    body,
                    successHints = listOf(
                        "snooze=1",
                        "vacation=1",
                        "snooze_cleared=1",
                        "vacation_cleared=1",
                    ),
                )
            }
        }

    suspend fun createShareLink(
        label: String,
        scope: String,
        expiresDays: Int,
    ): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/share")
                    .add("action", "create")
                    .add("label", label.trim())
                    .add("scope", scope)
                    .add("expires_days", expiresDays.toString())
                    .build()
                postFormForLocation(
                    "$root/api/share/manage",
                    body,
                    successHints = listOf("created=1"),
                ).let { location ->
                    Regex("[?&]new_token=([^&]+)").find(location)?.groupValues?.get(1)
                        ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
                }
            }
        }

    suspend fun revokeShareLink(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/share")
                    .add("action", "revoke")
                    .add("id", id)
                    .build()
                postForm(
                    "$root/api/share/manage",
                    body,
                    successHints = listOf("revoked=1"),
                )
            }
        }

    private fun postFormForLocation(
        url: String,
        body: FormBody,
        successHints: List<String>,
    ): String {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            val location = response.header("Location").orEmpty()
            val code = response.code
            val okRedirect = code in 300..399 && successHints.any { location.contains(it) }
            if (!okRedirect) {
                if (location.contains("/signin")) {
                    error("Session expired — sign in again")
                }
                if (location.contains("pro_required") || location.contains("error=pro")) {
                    error("Pro plan required for share links")
                }
                if (location.contains("viewer") || location.contains("manager_required")) {
                    error("Your household role cannot make this change")
                }
                error("Save failed (HTTP $code${if (location.isNotBlank()) ": $location" else ""})")
            }
            return location
        }
    }

    suspend fun renameDevice(deviceId: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/temperature")
                    .add("action", "rename")
                    .add("device_id", deviceId)
                    .add("name", name.trim())
                    .build()
                postForm(
                    "$root/api/devices",
                    body,
                    successHints = listOf("device_renamed=1"),
                )
            }
        }

    suspend fun setDeviceSpace(deviceId: String, space: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/temperature")
                    .add("action", "set_space")
                    .add("device_id", deviceId)
                    .add("space", space?.trim().orEmpty())
                    .build()
                postForm(
                    "$root/api/devices",
                    body,
                    successHints = listOf("device_renamed=1"),
                )
            }
        }

    suspend fun renameHousehold(name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/household")
                    .add("action", "rename")
                    .add("name", name.trim())
                    .build()
                postForm(
                    "$root/api/household",
                    body,
                    successHints = listOf("saved=1"),
                )
            }
        }

    suspend fun inviteHouseholdMember(email: String, role: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/household")
                    .add("action", "invite")
                    .add("email", email.trim())
                    .add("role", role)
                    .build()
                postForm(
                    "$root/api/household",
                    body,
                    successHints = listOf("invited=1"),
                )
            }
        }

    suspend fun switchHousehold(householdId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("redirect", "/dashboard/household")
                    .add("action", "switch")
                    .add("household_id", householdId)
                    .build()
                postForm(
                    "$root/api/household",
                    body,
                    successHints = listOf("switched=1"),
                )
            }
        }

    private fun postForm(url: String, body: FormBody, successHints: List<String>) {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            val location = response.header("Location").orEmpty()
            val code = response.code
            val okRedirect = code in 300..399 && successHints.any { location.contains(it) }
            val okDirect = response.isSuccessful && successHints.isEmpty()
            if (!okRedirect && !okDirect) {
                if (location.contains("/signin")) {
                    error("Session expired — sign in again")
                }
                if (location.contains("alert_error") || location.contains("prefs_error")) {
                    error("Server rejected the save")
                }
                if (location.contains("pro_required") || location.contains("error=pro")) {
                    error("Pro plan required")
                }
                if (location.contains("viewer") || location.contains("manager_required")) {
                    error("Your household role cannot make this change")
                }
                if (location.contains("editor") || location.contains("forbidden")) {
                    error("Household editor role required")
                }
                if (location.contains("missing_email")) {
                    error("Email is required")
                }
                error("Save failed (HTTP $code${if (location.isNotBlank()) ": $location" else ""})")
            }
        }
    }
}

private fun FormBody.Builder.addCheckbox(name: String, enabled: Boolean): FormBody.Builder {
    // Web form uses value="true" when checked; omitted when unchecked.
    // prepareAlertSettingsFormData sets empty string for missing keys — formCheckbox
    // only treats exact "true" as on, so omit when false.
    if (enabled) add(name, "true")
    return this
}

private fun FormBody.Builder.addNullable(name: String, value: String?): FormBody.Builder {
    if (value != null) add(name, value)
    return this
}
