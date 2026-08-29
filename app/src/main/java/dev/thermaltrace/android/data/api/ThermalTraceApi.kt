package dev.thermaltrace.android.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.thermaltrace.android.data.model.HomeReadingsResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface ThermalTraceApi {
    @GET("api/home/readings")
    suspend fun homeReadings(
        @Query("save") save: Int = 0,
        @Query("space") space: String? = null,
    ): Response<HomeReadingsResponse>
}

fun createThermalTraceApi(
    baseUrl: String,
    client: OkHttpClient,
    json: Json,
): ThermalTraceApi {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(ThermalTraceApi::class.java)
}
