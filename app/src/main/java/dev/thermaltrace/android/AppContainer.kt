package dev.thermaltrace.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.thermaltrace.android.data.api.ReadingsRepository
import dev.thermaltrace.android.data.api.SessionCookieJar
import dev.thermaltrace.android.data.api.ThermalTraceApi
import dev.thermaltrace.android.data.api.createThermalTraceApi
import dev.thermaltrace.android.data.auth.AuthRepository
import dev.thermaltrace.android.data.auth.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "thermaltrace_session",
)

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val sessionStore = SessionStore(appContext.sessionDataStore)

    val cookieJar = SessionCookieJar(
        baseHost = BuildConfig.THERMALTRACE_BASE_URL
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/'),
        sessionStore = sessionStore,
    )

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    val api: ThermalTraceApi = createThermalTraceApi(
        baseUrl = BuildConfig.THERMALTRACE_BASE_URL.trimEnd('/') + "/",
        client = okHttpClient,
        json = json,
    )

    val readingsRepository = ReadingsRepository(api)

    val authRepository = AuthRepository(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        sessionStore = sessionStore,
        cookieJar = cookieJar,
    )
}
