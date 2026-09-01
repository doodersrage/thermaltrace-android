package dev.thermaltrace.android.ui.login

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
    val yubikeyOtp: String = "",
    val loading: Boolean = false,
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

    fun onYubikeyOtpChange(value: String) {
        _uiState.update { it.copy(yubikeyOtp = value.trim(), error = null) }
    }

    fun verify(onSuccess: () -> Unit) {
        val state = _uiState.value
        val yubikeyOtp = state.yubikeyOtp
        val code = state.code

        if (yubikeyOtp.isNotEmpty()) {
            if (yubikeyOtp.length < 44) {
                _uiState.update { it.copy(error = "Tap your YubiKey to fill the OTP field") }
                return
            }
        } else if (code.length != 6) {
            _uiState.update { it.copy(error = "Enter the 6-digit code or a YubiKey OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.verifyMfa(
                code = code,
                yubikeyOtp = yubikeyOtp.takeIf { it.isNotEmpty() },
            )
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Verification failed")
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
