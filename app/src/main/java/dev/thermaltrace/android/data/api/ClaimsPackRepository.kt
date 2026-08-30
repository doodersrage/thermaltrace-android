package dev.thermaltrace.android.data.api

import android.content.Context
import dev.thermaltrace.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

class ClaimsPackRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun download(days: Int): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val to = LocalDate.now(ZoneOffset.UTC)
            val from = to.minusDays((days - 1).coerceAtLeast(0).toLong())
            val url =
                "${BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/')}/api/claims/pack?from=$from&to=$to"
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    401 -> error("Unauthorized — sign in again")
                    403 -> error("Pro plan required for the claims pack")
                }
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: ${response.body?.string().orEmpty().take(200)}")
                }
                val body = response.body ?: error("Empty claims pack response")
                val dir = File(context.cacheDir, "claims").apply { mkdirs() }
                val file = File(dir, "thermaltrace-claims-$from-to-$to.html")
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
                file
            }
        }
    }
}
