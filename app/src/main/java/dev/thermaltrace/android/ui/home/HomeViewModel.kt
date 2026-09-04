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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

private enum class HomeRefreshMode { Full, Live }

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
    private var refreshJob: Job? = null
    private var pollCount = 0

    init {
        refresh(mode = HomeRefreshMode.Full)
        startPolling()
    }

    fun selectSpace(space: String?) {
        _uiState.update { it.copy(selectedSpace = space) }
        refresh(mode = HomeRefreshMode.Full)
    }

    fun refresh(initial: Boolean = false) {
        refresh(mode = if (initial) HomeRefreshMode.Full else HomeRefreshMode.Live)
    }

    private fun refresh(mode: HomeRefreshMode) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasReadings = _uiState.value.readings != null
            _uiState.update {
                when {
                    !hasReadings -> it.copy(loading = true, error = null)
                    else -> it.copy(refreshing = true, error = null)
                }
            }

            val space = _uiState.value.selectedSpace
            when (mode) {
                HomeRefreshMode.Live -> refreshLive(space)
                HomeRefreshMode.Full -> refreshFull(space)
            }
        }
    }

    private suspend fun refreshLive(space: String?) = coroutineScope {
        val readingsDeferred = async { readingsRepository.fetchHomeReadings(space) }
        val ingestDeferred = async { dashboardExtrasRepository.loadIngestStatus() }
        val doorsDeferred = async { dashboardExtrasRepository.loadDoorEvents() }

        val readingsResult = readingsDeferred.await()
        readingsResult.fold(
            onSuccess = { body ->
                _uiState.update {
                    it.copy(loading = false, readings = body, error = null)
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = err.message ?: "Failed to load readings",
                    )
                }
            },
        )

        val ingest = ingestDeferred.await().getOrNull()
        val doors = doorsDeferred.await().getOrNull()
        _uiState.update {
            it.copy(
                refreshing = false,
                ingestStatus = ingest ?: it.ingestStatus,
                doorSessions = doors?.liveSessions ?: it.doorSessions,
            )
        }
    }

    private suspend fun refreshFull(space: String?) = coroutineScope {
        val readingsDeferred = async { readingsRepository.fetchHomeReadings(space) }
        val exportDeferred = async { settingsRepository.loadExport() }
        val historyDeferred = async { historyRepository.load(days = 7) }
        val insightsDeferred = async { dashboardExtrasRepository.loadHomeInsights() }
        val doorsDeferred = async { dashboardExtrasRepository.loadDoorEvents() }
        val ingestDeferred = async { dashboardExtrasRepository.loadIngestStatus() }

        val readingsResult = readingsDeferred.await()
        readingsResult.fold(
            onSuccess = { body ->
                _uiState.update {
                    it.copy(loading = false, readings = body, error = null)
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = err.message ?: "Failed to load readings",
                    )
                }
            },
        )

        val export = exportDeferred.await().getOrNull()
        val prefs = export?.preferences
        val freezeThreshold = export?.alertSettings?.freezeThresholdF ?: 34.0
        val history = historyDeferred.await().getOrNull()
        val homeInsights = insightsDeferred.await().getOrNull()
        val doorEvents = doorsDeferred.await().getOrNull()
        val ingestStatus = ingestDeferred.await().getOrNull()

        val outdoor = runCatching {
            val cityId = prefs?.weatherCityId
            val weatherResponse = api.weather(cityId)
            if (weatherResponse.isSuccessful) weatherResponse.body()?.weather?.temp else null
        }.getOrNull()
            ?: homeInsights?.outdoorTempF
            ?: homeInsights?.weather?.tempF

        val insights = buildHeatingInsights(
            indoorPoints = history?.chart?.points.orEmpty(),
            outdoorTempF = outdoor,
            freezeThresholdF = freezeThreshold,
            house = homeInsights?.house,
        )

        _uiState.update {
            it.copy(
                loading = false,
                refreshing = false,
                insights = insights,
                outdoorTempF = outdoor,
                homeInsights = homeInsights ?: it.homeInsights,
                doorSessions = doorEvents?.liveSessions ?: it.doorSessions,
                ingestStatus = ingestStatus ?: it.ingestStatus,
            )
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                pollCount += 1
                // Full extras every ~2.5 minutes; live readings more often.
                refresh(
                    mode = if (pollCount % 5 == 0) HomeRefreshMode.Full else HomeRefreshMode.Live,
                )
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        refreshJob?.cancel()
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
