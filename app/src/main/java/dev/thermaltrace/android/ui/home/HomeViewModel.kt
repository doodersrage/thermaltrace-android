package dev.thermaltrace.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.ReadingsRepository
import dev.thermaltrace.android.data.auth.AuthRepository
import dev.thermaltrace.android.data.model.HomeReadingsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val readings: HomeReadingsResponse? = null,
    val selectedSpace: String? = null,
    val error: String? = null,
)

class HomeViewModel(
    private val readingsRepository: ReadingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh(initial = true)
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
            val result = readingsRepository.fetchHomeReadings(space)
            result.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            readings = body,
                            error = null,
                        )
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
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }

    companion object {
        fun factory(
            readingsRepository: ReadingsRepository,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(readingsRepository, authRepository) as T
            }
    }
}
