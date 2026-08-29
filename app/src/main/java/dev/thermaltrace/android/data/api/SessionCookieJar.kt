package dev.thermaltrace.android.data.api

import android.util.Log
import dev.thermaltrace.android.data.auth.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Mirrors the web dashboard session: ThermalTrace validates
 * `sb-access-token` + `sb-refresh-token` cookies (not Bearer JWT).
 */
class SessionCookieJar(
    private val baseHost: String,
    private val sessionStore: SessionStore,
) : CookieJar {
    @Volatile
    private var memoryCookies: List<Cookie> = emptyList()

    fun syncFromSession() {
        val tokens = runBlocking { sessionStore.current() } ?: run {
            memoryCookies = emptyList()
            return
        }
        memoryCookies = listOf(
            buildCookie("sb-access-token", tokens.accessToken),
            buildCookie("sb-refresh-token", tokens.refreshToken),
        )
    }

    fun clear() {
        memoryCookies = emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!url.host.endsWith(baseHost)) return
        val kept = memoryCookies.associateBy { it.name }.toMutableMap()
        for (cookie in cookies) {
            if (cookie.name == "sb-access-token" || cookie.name == "sb-refresh-token") {
                kept[cookie.name] = cookie
            }
        }
        memoryCookies = kept.values.toList()
        val access = kept["sb-access-token"]?.value
        val refresh = kept["sb-refresh-token"]?.value
        if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
            runBlocking {
                sessionStore.save(
                    dev.thermaltrace.android.data.auth.SessionTokens(access, refresh),
                )
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.host.endsWith(baseHost)) return emptyList()
        if (memoryCookies.isEmpty()) {
            syncFromSession()
        }
        Log.d(TAG, "Attaching ${memoryCookies.size} session cookies to ${url.encodedPath}")
        return memoryCookies
    }

    private fun buildCookie(name: String, value: String): Cookie =
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain(baseHost)
            .path("/")
            .secure()
            .httpOnly()
            .build()

    companion object {
        private const val TAG = "SessionCookieJar"
    }
}
