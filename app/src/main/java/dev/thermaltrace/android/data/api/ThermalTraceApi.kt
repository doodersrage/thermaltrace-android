package dev.thermaltrace.android.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.thermaltrace.android.data.model.AckResponse
import dev.thermaltrace.android.data.model.AlertEventsResponse
import dev.thermaltrace.android.data.model.HistoryResponse
import dev.thermaltrace.android.data.model.HomeReadingsResponse
import dev.thermaltrace.android.data.model.HouseholdResponse
import dev.thermaltrace.android.data.model.ShareLinksResponse
import dev.thermaltrace.android.data.model.TestAlertResponse
import dev.thermaltrace.android.data.model.UserExportResponse
import dev.thermaltrace.android.data.model.WeatherResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ThermalTraceApi {
    @GET("api/home/readings")
    suspend fun homeReadings(
        @Query("save") save: Int = 0,
        @Query("space") space: String? = null,
    ): Response<HomeReadingsResponse>

    @GET("api/user/export")
    suspend fun userExport(): Response<UserExportResponse>

    @GET("api/household")
    suspend fun household(): Response<HouseholdResponse>

    @GET("api/user/alert-events")
    suspend fun alertEvents(
        @Query("limit") limit: Int = 30,
    ): Response<AlertEventsResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/user/alert-events/ack")
    suspend fun ackAlertEvent(
        @Body body: RequestBody,
    ): Response<AckResponse>

    @Headers("Accept: application/json")
    @POST("api/user/alert-test")
    suspend fun testAlert(): Response<TestAlertResponse>

    @GET("api/user/history")
    suspend fun history(
        @Query("days") days: Int = 7,
        @Query("include") include: String = "chart,readings,filters",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("probe") probe: String? = null,
    ): Response<HistoryResponse>

    @GET("api/home/weather")
    suspend fun weather(
        @Query("cityId") cityId: String? = null,
    ): Response<WeatherResponse>

    @GET("api/share/manage")
    suspend fun shareLinks(): Response<ShareLinksResponse>
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
