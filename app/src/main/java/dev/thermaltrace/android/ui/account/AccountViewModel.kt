package dev.thermaltrace.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.auth.AuthRepository
import dev.thermaltrace.android.data.model.UserPreferencesDto
import dev.thermaltrace.android.data.push.PushRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val email: String? = null,
    val preferences: UserPreferencesDto = UserPreferencesDto(),
    val pushConfigured: Boolean = false,
    val pushBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AccountViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val pushRegistrar: PushRegistrar,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AccountUiState(pushConfigured = pushRegistrar.isConfigured),
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            settingsRepository.loadExport().fold(
                onSuccess = { export ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            email = export.user?.email,
                            preferences = export.preferences ?: UserPreferencesDto(),
                            pushConfigured = pushRegistrar.isConfigured,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(loading = false, error = err.message ?: "Failed to load account")
                    }
                },
            )
        }
    }

    fun update(transform: (UserPreferencesDto) -> UserPreferencesDto) {
        _uiState.update {
            it.copy(preferences = transform(it.preferences), message = null, error = null)
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            settingsRepository.savePreferences(_uiState.value.preferences).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "Preferences saved") }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Save failed")
                    }
                },
            )
        }
    }

    fun enablePush() {
        viewModelScope.launch {
            _uiState.update { it.copy(pushBusy = true, error = null, message = null) }
            pushRegistrar.registerWithServer().fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            pushBusy = false,
                            message = "Push registered. Enable “Push” under Alerts on the website (Pro).",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(pushBusy = false, error = err.message ?: "Push registration failed")
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
            settingsRepository: SettingsRepository,
            authRepository: AuthRepository,
            pushRegistrar: PushRegistrar,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AccountViewModel(settingsRepository, authRepository, pushRegistrar) as T
            }
    }
}
