package dev.thermaltrace.android.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.api.ShareRepository
import dev.thermaltrace.android.data.model.ShareLinkDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val canCreate: Boolean = false,
    val links: List<ShareLinkDto> = emptyList(),
    val label: String = "",
    val scope: String = "live",
    val expiresDays: Int = 7,
    val message: String? = null,
    val error: String? = null,
)

class ShareViewModel(
    private val shareRepository: ShareRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setLabel(value: String) = _uiState.update { it.copy(label = value) }
    fun setScope(value: String) = _uiState.update { it.copy(scope = value) }
    fun setExpiresDays(value: Int) = _uiState.update { it.copy(expiresDays = value) }

    fun publicUrl(token: String): String = shareRepository.publicUrl(token)

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val entitlements = settingsRepository.loadExport().getOrNull()?.entitlements
            val linksResult = shareRepository.listLinks()
            _uiState.update { state ->
                state.copy(
                    loading = false,
                    canCreate = entitlements?.canCreateShareLinks == true,
                    links = linksResult.getOrElse { state.links },
                    error = linksResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun create() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(busy = true, error = null, message = null) }
            shareRepository.create(state.label, state.scope, state.expiresDays).fold(
                onSuccess = { token ->
                    _uiState.update {
                        it.copy(
                            busy = false,
                            message = if (token != null) {
                                "Created — copy the new link below"
                            } else {
                                "Share link created"
                            },
                            label = "",
                        )
                    }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(busy = false, error = err.message ?: "Create failed")
                    }
                },
            )
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, message = null) }
            shareRepository.revoke(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(busy = false, message = "Link revoked") }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(busy = false, error = err.message ?: "Revoke failed")
                    }
                },
            )
        }
    }

    companion object {
        fun factory(
            shareRepository: ShareRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShareViewModel(shareRepository, settingsRepository) as T
            }
    }
}
