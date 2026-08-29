package dev.thermaltrace.android.data.auth

import dev.thermaltrace.android.data.api.SessionCookieJar
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    supabaseUrl: String,
    supabaseAnonKey: String,
    private val sessionStore: SessionStore,
    private val cookieJar: SessionCookieJar,
) {
    private val client: SupabaseClient? = if (
        supabaseUrl.isBlank() || supabaseAnonKey.isBlank()
    ) {
        null
    } else {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey,
        ) {
            install(Auth)
        }
    }

    val isConfigured: Boolean get() = client != null

    val sessionFlow: Flow<SessionTokens?> = sessionStore.sessionFlow

    val isSignedIn: Flow<Boolean> = sessionFlow.map { it != null }

    suspend fun restoreSession() {
        val tokens = sessionStore.current() ?: return
        cookieJar.syncFromSession()
        val supabase = client ?: return
        runCatching {
            supabase.auth.importSession(
                io.github.jan.supabase.auth.user.UserSession(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = null,
                ),
            )
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        val supabase = client
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase is not configured. Set supabase.url and supabase.anonKey in local.properties.",
                ),
            )

        return runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
                ?: error("Sign-in succeeded but no session was returned")
            val tokens = SessionTokens(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            )
            sessionStore.save(tokens)
            cookieJar.syncFromSession()
        }
    }

    suspend fun signOut() {
        runCatching { client?.auth?.signOut() }
        sessionStore.clear()
        cookieJar.clear()
    }

    fun sessionStatus(): Flow<SessionStatus>? = client?.auth?.sessionStatus
}
