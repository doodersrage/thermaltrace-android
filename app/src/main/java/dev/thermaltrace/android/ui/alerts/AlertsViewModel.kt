package dev.thermaltrace.android.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.AlertsInboxRepository
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.model.AlertEventDto
import dev.thermaltrace.android.data.model.AlertSettingsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlertsTab { Inbox, Settings }

data class AlertsUiState(
    val tab: AlertsTab = AlertsTab.Inbox,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val settings: AlertSettingsDto = AlertSettingsDto(),
    val events: List<AlertEventDto> = emptyList(),
    val unackedCount: Int = 0,
    val highlightEventId: Long? = null,
    val message: String? = null,
    val error: String? = null,
)

class AlertsViewModel(
    private val settingsRepository: SettingsRepository,
    private val inboxRepository: AlertsInboxRepository,
    highlightEventId: Long? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AlertsUiState(
            tab = AlertsTab.Inbox,
            highlightEventId = highlightEventId,
        ),
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setHighlightEventId(eventId: Long?) {
        _uiState.update { it.copy(highlightEventId = eventId) }
    }

    fun selectTab(tab: AlertsTab) {
        _uiState.update { it.copy(tab = tab, message = null, error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val settingsResult = settingsRepository.loadExport()
            val eventsResult = inboxRepository.listEvents()
            _uiState.update { state ->
                state.copy(
                    loading = false,
                    settings = settingsResult.getOrNull()?.alertSettings ?: state.settings,
                    events = eventsResult.getOrNull()?.first ?: state.events,
                    unackedCount = eventsResult.getOrNull()?.second ?: state.unackedCount,
                    error = settingsResult.exceptionOrNull()?.message
                        ?: eventsResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun update(transform: (AlertSettingsDto) -> AlertSettingsDto) {
        _uiState.update { it.copy(settings = transform(it.settings), message = null, error = null) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            settingsRepository.saveAlertSettings(_uiState.value.settings).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "Alert settings saved") }
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

    fun acknowledge(eventId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            inboxRepository.acknowledge(eventId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(saving = false, message = "Acknowledged", highlightEventId = null)
                    }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Ack failed")
                    }
                },
            )
        }
    }

    fun sendTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            inboxRepository.sendTest().fold(
                onSuccess = { msg ->
                    _uiState.update { it.copy(saving = false, message = msg) }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Test failed")
                    }
                },
            )
        }
    }

    fun snoozeHours(hours: Int) = runAction { settingsRepository.snoozeHours(hours) }
    fun vacationDays(days: Int) = runAction { settingsRepository.vacationDays(days) }
    fun snooze24() = snoozeHours(24)
    fun vacation7() = vacationDays(7)
    fun clearSnooze() = runAction { settingsRepository.clearSnooze() }
    fun clearVacation() = runAction { settingsRepository.clearVacation() }

    private fun runAction(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            block().fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "Updated") }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Action failed")
                    }
                },
            )
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            inboxRepository: AlertsInboxRepository,
            highlightEventId: Long? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AlertsViewModel(settingsRepository, inboxRepository, highlightEventId) as T
            }
    }
}
