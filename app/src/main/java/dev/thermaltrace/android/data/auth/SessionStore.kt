package dev.thermaltrace.android.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
)

class SessionStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val accessKey = stringPreferencesKey("sb_access_token")
    private val refreshKey = stringPreferencesKey("sb_refresh_token")

    val sessionFlow: Flow<SessionTokens?> = dataStore.data.map { prefs ->
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            null
        } else {
            SessionTokens(access, refresh)
        }
    }

    suspend fun current(): SessionTokens? = sessionFlow.first()

    suspend fun save(tokens: SessionTokens) {
        dataStore.edit { prefs ->
            prefs[accessKey] = tokens.accessToken
            prefs[refreshKey] = tokens.refreshToken
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(accessKey)
            prefs.remove(refreshKey)
        }
    }
}
