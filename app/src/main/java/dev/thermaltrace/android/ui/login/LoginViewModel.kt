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

enum class LoginMode { SignIn, Register, ForgotPassword }

data class LoginUiState(
    val mode: LoginMode = LoginMode.SignIn,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val configured: Boolean = true,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LoginUiState(configured = authRepository.isConfigured),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null, message = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null, message = null) }
    }

    fun setMode(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, error = null, message = null) }
    }

    fun oauthUrl(provider: String): String = authRepository.oauthStartUrl(provider)

    fun submit(onSignedIn: () -> Unit, onNeedsMfa: () -> Unit) {
        when (_uiState.value.mode) {
            LoginMode.SignIn -> signIn(onSignedIn, onNeedsMfa)
            LoginMode.Register -> signUp()
            LoginMode.ForgotPassword -> resetPassword()
        }
    }

    fun signIn(onSuccess: () -> Unit, onNeedsMfa: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            val result = authRepository.signIn(state.email, state.password)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = { outcome ->
                    if (outcome.needsMfa) onNeedsMfa() else onSuccess()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Sign-in failed")
                    }
                },
            )
        }
    }

    fun exchangeOAuth(token: String, onSuccess: () -> Unit, onNeedsMfa: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            val result = authRepository.exchangeMobileOAuth(token)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = { outcome ->
                    if (outcome.needsMfa) onNeedsMfa() else onSuccess()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "OAuth sign-in failed")
                    }
                },
            )
        }
    }

    private fun signUp() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            val result = authRepository.signUp(state.email, state.password)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            message = "Account created. Check your email if confirmation is required.",
                            mode = LoginMode.SignIn,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Sign-up failed")
                    }
                },
            )
        }
    }

    private fun resetPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(error = "Enter your account email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            val result = authRepository.resetPassword(email)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            message = "Password reset email sent if an account exists.",
                            mode = LoginMode.SignIn,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "Reset failed")
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
                    LoginViewModel(authRepository) as T
            }
    }
}
