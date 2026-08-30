package dev.thermaltrace.android.data.api

import dev.thermaltrace.android.BuildConfig
import dev.thermaltrace.android.data.model.ShareLinkDto

class ShareRepository(
    private val api: ThermalTraceApi,
    private val formClient: SettingsFormClient,
) {
    suspend fun listLinks(): Result<List<ShareLinkDto>> = runCatching {
        val response = api.shareLinks()
        if (response.code() == 401) error("Unauthorized — sign in again")
        if (!response.isSuccessful) {
            error("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }
        response.body()?.links.orEmpty()
    }

    suspend fun create(
        label: String,
        scope: String,
        expiresDays: Int,
    ): Result<String?> = formClient.createShareLink(label, scope, expiresDays)

    suspend fun revoke(id: String): Result<Unit> = formClient.revokeShareLink(id)

    fun publicUrl(token: String): String =
        "${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/share/$token"
}
