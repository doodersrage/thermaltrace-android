package dev.thermaltrace.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.HistoryRepository
import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.data.model.HistoryReadingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val days: Int = 7,
    val points: List<ChartPointDto> = emptyList(),
    val readings: List<HistoryReadingDto> = emptyList(),
    val error: String? = null,
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh(7)
    }

    fun refresh(days: Int = _uiState.value.days) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, days = days, error = null) }
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

    companion object {
        fun factory(historyRepository: HistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(historyRepository) as T
            }
    }
}
