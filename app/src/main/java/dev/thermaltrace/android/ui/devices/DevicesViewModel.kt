package dev.thermaltrace.android.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.DashboardExtrasRepository
import dev.thermaltrace.android.data.api.DevicesRepository
import dev.thermaltrace.android.data.model.DeviceSummary
import dev.thermaltrace.android.data.model.IngestStatusResponse
import dev.thermaltrace.android.data.model.ThermostatStatusResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DevicesUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val devices: List<DeviceSummary> = emptyList(),
    val thermostat: ThermostatStatusResponse? = null,
    val ingestStatus: IngestStatusResponse? = null,
    val editingId: String? = null,
    val draftName: String = "",
    val draftSpace: String = "",
    val message: String? = null,
    val error: String? = null,
)

class DevicesViewModel(
    private val devicesRepository: DevicesRepository,
    private val dashboardExtrasRepository: DashboardExtrasRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()
    private var pollJob: Job? = null

    init {
        refresh()
        startIngestPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            val devicesResult = devicesRepository.listDevices()
            val thermostat = dashboardExtrasRepository.loadThermostatStatus().getOrNull()
            val ingest = dashboardExtrasRepository.loadIngestStatus().getOrNull()
            devicesResult.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            devices = list,
                            thermostat = thermostat,
                            ingestStatus = ingest,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            thermostat = thermostat,
                            ingestStatus = ingest,
                            error = err.message ?: "Failed to load devices",
                        )
                    }
                },
            )
        }
    }

    private fun startIngestPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                val ingest = dashboardExtrasRepository.loadIngestStatus().getOrNull()
                if (ingest != null) {
                    _uiState.update { it.copy(ingestStatus = ingest) }
                }
            }
        }
    }

    fun startEdit(device: DeviceSummary) {
        _uiState.update {
            it.copy(
                editingId = device.id,
                draftName = device.name,
                draftSpace = device.space.orEmpty(),
                message = null,
                error = null,
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingId = null) }
    }

    fun onDraftName(value: String) {
        _uiState.update { it.copy(draftName = value) }
    }

    fun onDraftSpace(value: String) {
        _uiState.update { it.copy(draftSpace = value) }
    }

    fun saveEdit() {
        val state = _uiState.value
        val id = state.editingId ?: return
        val name = state.draftName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            val rename = devicesRepository.renameDevice(id, name)
            if (rename.isFailure) {
                _uiState.update {
                    it.copy(saving = false, error = rename.exceptionOrNull()?.message ?: "Rename failed")
                }
                return@launch
            }
            val space = state.draftSpace.trim().ifBlank { null }
            val setSpace = devicesRepository.setDeviceSpace(id, space)
            _uiState.update {
                it.copy(
                    saving = false,
                    editingId = null,
                    message = if (setSpace.isSuccess) "Device updated" else "Renamed; space update failed",
                    error = setSpace.exceptionOrNull()?.message,
                )
            }
            refresh()
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(
            devicesRepository: DevicesRepository,
            dashboardExtrasRepository: DashboardExtrasRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DevicesViewModel(devicesRepository, dashboardExtrasRepository) as T
            }
    }
}
