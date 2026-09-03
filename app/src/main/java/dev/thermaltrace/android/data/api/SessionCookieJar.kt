package dev.thermaltrace.android.data.api

import android.util.Log
import dev.thermaltrace.android.data.auth.SessionStore
import dev.thermaltrace.android.data.auth.SessionTokens
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Mirrors the web dashboard session: ThermalTrace validates
 * `sb-access-token` + `sb-refresh-token` cookies (not Bearer JWT).
 * `sb-mfa-required=0` is required after YubiKey OTP (AAL stays aal1).
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
        val cookies = mutableListOf(
            buildCookie(ACCESS_COOKIE, tokens.accessToken),
            buildCookie(REFRESH_COOKIE, tokens.refreshToken),
        )
        if (tokens.mfaSatisfied) {
            cookies += buildCookie(MFA_COOKIE, MFA_SATISFIED_VALUE)
        }
        memoryCookies = cookies
    }

    fun clear() {
        memoryCookies = emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!url.host.endsWith(baseHost)) return
        val kept = memoryCookies.associateBy { it.name }.toMutableMap()
        for (cookie in cookies) {
            if (cookie.name in SESSION_COOKIE_NAMES) {
                kept[cookie.name] = cookie
            }
        }
        memoryCookies = kept.values.toList()
        val access = kept[ACCESS_COOKIE]?.value
        val refresh = kept[REFRESH_COOKIE]?.value
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) return

        val mfaValue = kept[MFA_COOKIE]?.value
        val existing = runBlocking { sessionStore.current() }
        val mfaSatisfied = when (mfaValue) {
            MFA_SATISFIED_VALUE -> true
            MFA_REQUIRED_VALUE -> false
            else -> existing?.mfaSatisfied == true
        }
        runBlocking {
            sessionStore.save(
                SessionTokens(
                    accessToken = access,
                    refreshToken = refresh,
                    mfaSatisfied = mfaSatisfied,
                ),
            )
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
        const val ACCESS_COOKIE = "sb-access-token"
        const val REFRESH_COOKIE = "sb-refresh-token"
        const val MFA_COOKIE = "sb-mfa-required"
        const val MFA_SATISFIED_VALUE = "0"
        const val MFA_REQUIRED_VALUE = "1"
        private val SESSION_COOKIE_NAMES = setOf(ACCESS_COOKIE, REFRESH_COOKIE, MFA_COOKIE)
    }
}
