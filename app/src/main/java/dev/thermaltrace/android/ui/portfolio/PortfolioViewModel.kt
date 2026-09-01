package dev.thermaltrace.android.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.DashboardExtrasRepository
import dev.thermaltrace.android.data.api.HouseholdRepository
import dev.thermaltrace.android.data.model.PortfolioProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val loading: Boolean = true,
    val switchingId: String? = null,
    val properties: List<PortfolioProperty> = emptyList(),
    val error: String? = null,
    val message: String? = null,
)

class PortfolioViewModel(
    private val dashboardExtrasRepository: DashboardExtrasRepository,
    private val householdRepository: HouseholdRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            dashboardExtrasRepository.loadPortfolio().fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            properties = body.properties,
                            error = body.error,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(loading = false, error = err.message ?: "Failed to load portfolio")
                    }
                },
            )
        }
    }

    fun switchTo(householdId: String, onSwitched: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(switchingId = householdId, error = null, message = null) }
            householdRepository.switchTo(householdId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(switchingId = null, message = "Switched property")
                    }
                    onSwitched()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            switchingId = null,
                            error = err.message ?: "Switch failed",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(
            dashboardExtrasRepository: DashboardExtrasRepository,
            householdRepository: HouseholdRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PortfolioViewModel(dashboardExtrasRepository, householdRepository) as T
            }
    }
}
