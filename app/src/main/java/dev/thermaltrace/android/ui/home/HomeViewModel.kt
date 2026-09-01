package dev.thermaltrace.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.DashboardExtrasRepository
import dev.thermaltrace.android.data.api.HistoryRepository
import dev.thermaltrace.android.data.api.ReadingsRepository
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.api.ThermalTraceApi
import dev.thermaltrace.android.data.insights.HeatingInsight
import dev.thermaltrace.android.data.insights.buildHeatingInsights
import dev.thermaltrace.android.data.model.DoorSession
import dev.thermaltrace.android.data.model.HomeInsightsResponse
import dev.thermaltrace.android.data.model.HomeReadingsResponse
import dev.thermaltrace.android.data.model.IngestStatusResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val readings: HomeReadingsResponse? = null,
    val selectedSpace: String? = null,
    val insights: List<HeatingInsight> = emptyList(),
    val outdoorTempF: Double? = null,
    val homeInsights: HomeInsightsResponse? = null,
    val doorSessions: List<DoorSession> = emptyList(),
    val ingestStatus: IngestStatusResponse? = null,
    val error: String? = null,
)

class HomeViewModel(
    private val readingsRepository: ReadingsRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val dashboardExtrasRepository: DashboardExtrasRepository,
    private val api: ThermalTraceApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var pollJob: Job? = null

    init {
        refresh(initial = true)
        startPolling()
    }

    fun selectSpace(space: String?) {
        _uiState.update { it.copy(selectedSpace = space) }
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (initial) it.copy(loading = true, error = null)
                else it.copy(refreshing = true, error = null)
            }
            val space = _uiState.value.selectedSpace
            val readingsResult = readingsRepository.fetchHomeReadings(space)
            val export = settingsRepository.loadExport().getOrNull()
            val prefs = export?.preferences
            val freezeThreshold = export?.alertSettings?.freezeThresholdF ?: 34.0
            val history = historyRepository.load(days = 7).getOrNull()
            val homeInsights = dashboardExtrasRepository.loadHomeInsights().getOrNull()
            val doorEvents = dashboardExtrasRepository.loadDoorEvents().getOrNull()
            val ingestStatus = dashboardExtrasRepository.loadIngestStatus().getOrNull()
            val outdoor = runCatching {
                val cityId = prefs?.weatherCityId
                val weatherResponse = api.weather(cityId)
                if (weatherResponse.isSuccessful) weatherResponse.body()?.weather?.temp else null
            }.getOrNull()
                ?: homeInsights?.outdoorTempF
                ?: homeInsights?.weather?.tempF

            val points = history?.chart?.points.orEmpty()
            val insights = buildHeatingInsights(
                indoorPoints = points,
                outdoorTempF = outdoor,
                freezeThresholdF = freezeThreshold,
                house = homeInsights?.house,
            )

            readingsResult.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            readings = body,
                            insights = insights,
                            outdoorTempF = outdoor,
                            homeInsights = homeInsights,
                            doorSessions = doorEvents?.liveSessions.orEmpty(),
                            ingestStatus = ingestStatus,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            insights = insights,
                            outdoorTempF = outdoor,
                            homeInsights = homeInsights,
                            doorSessions = doorEvents?.liveSessions.orEmpty(),
                            ingestStatus = ingestStatus,
                            error = err.message ?: "Failed to load readings",
                        )
                    }
                },
            )
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refresh(initial = false)
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(
            readingsRepository: ReadingsRepository,
            historyRepository: HistoryRepository,
            settingsRepository: SettingsRepository,
            dashboardExtrasRepository: DashboardExtrasRepository,
            api: ThermalTraceApi,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        readingsRepository,
                        historyRepository,
                        settingsRepository,
                        dashboardExtrasRepository,
                        api,
                    ) as T
            }
    }
}
