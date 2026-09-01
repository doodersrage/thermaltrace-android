package dev.thermaltrace.android.ui.login

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MfaUiState(
    val code: String = "",
    val loading: Boolean = false,
    val webauthnLoading: Boolean = false,
    val error: String? = null,
)

class MfaViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MfaUiState())
    val uiState: StateFlow<MfaUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = digits, error = null) }
    }

    fun verify(onSuccess: () -> Unit) {
        val code = _uiState.value.code
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Enter the 6-digit code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.verifyMfa(code)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Invalid code")
                    }
                },
            )
        }
    }

    fun verifyWithSecurityKey(activity: ComponentActivity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(webauthnLoading = true, error = null) }
            val result = authRepository.verifyMfaWithSecurityKey(activity)
            _uiState.update { it.copy(webauthnLoading = false) }
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Security key verification failed")
                    }
                },
            )
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MfaViewModel(authRepository) as T
            }
    }
}
