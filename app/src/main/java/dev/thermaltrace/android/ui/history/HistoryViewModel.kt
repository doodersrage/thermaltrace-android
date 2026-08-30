package dev.thermaltrace.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.ClaimsPackRepository
import dev.thermaltrace.android.data.api.HistoryRepository
import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.data.model.HistoryReadingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class HistoryUiState(
    val loading: Boolean = true,
    val days: Int = 7,
    val points: List<ChartPointDto> = emptyList(),
    val readings: List<HistoryReadingDto> = emptyList(),
    val error: String? = null,
    val claimsBusy: Boolean = false,
    val claimsMessage: String? = null,
    val claimsFile: File? = null,
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val claimsPackRepository: ClaimsPackRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh(7)
    }

    fun refresh(days: Int = _uiState.value.days) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, days = days, error = null, claimsMessage = null) }
            historyRepository.load(days = days).fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            points = body.chart?.points.orEmpty(),
                            readings = body.readings?.readings.orEmpty(),
                            error = body.chart?.error ?: body.readings?.error,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(loading = false, error = err.message ?: "Failed to load history")
                    }
                },
            )
        }
    }

    fun downloadClaimsPack() {
        viewModelScope.launch {
            _uiState.update { it.copy(claimsBusy = true, claimsMessage = null, error = null) }
            claimsPackRepository.download(_uiState.value.days).fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            claimsBusy = false,
                            claimsFile = file,
                            claimsMessage = "Claims pack ready",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            claimsBusy = false,
                            claimsMessage = null,
                            error = err.message ?: "Claims pack failed",
                        )
                    }
                },
            )
        }
    }

    fun clearClaimsFile() {
        _uiState.update { it.copy(claimsFile = null) }
    }

    companion object {
        fun factory(
            historyRepository: HistoryRepository,
            claimsPackRepository: ClaimsPackRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(historyRepository, claimsPackRepository) as T
            }
    }
}
