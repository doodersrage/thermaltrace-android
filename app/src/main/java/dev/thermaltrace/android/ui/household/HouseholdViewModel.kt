package dev.thermaltrace.android.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.HouseholdRepository
import dev.thermaltrace.android.data.model.HouseholdInviteDto
import dev.thermaltrace.android.data.model.HouseholdMemberDto
import dev.thermaltrace.android.data.model.UserHouseholdDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val householdId: String? = null,
    val households: List<UserHouseholdDto> = emptyList(),
    val members: List<HouseholdMemberDto> = emptyList(),
    val invites: List<HouseholdInviteDto> = emptyList(),
    val draftName: String = "",
    val inviteEmail: String = "",
    val inviteRole: String = "viewer",
    val message: String? = null,
    val error: String? = null,
)

class HouseholdViewModel(
    private val householdRepository: HouseholdRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val keepContent = _uiState.value.members.isNotEmpty() ||
                _uiState.value.households.isNotEmpty()
            _uiState.update { it.copy(loading = !keepContent, error = null, message = null) }
            householdRepository.load().fold(
                onSuccess = { data ->
                    val activeName = data.households
                        .firstOrNull { it.householdId == data.householdId }
                        ?.name
                        .orEmpty()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            householdId = data.householdId,
                            households = data.households,
                            members = data.members,
                            invites = data.invites,
                            draftName = activeName,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(loading = false, error = err.message ?: "Failed to load household")
                    }
                },
            )
        }
    }

    fun onDraftName(value: String) {
        _uiState.update { it.copy(draftName = value) }
    }

    fun onInviteEmail(value: String) {
        _uiState.update { it.copy(inviteEmail = value) }
    }

    fun onInviteRole(value: String) {
        _uiState.update { it.copy(inviteRole = value) }
    }

    fun saveName() {
        val name = _uiState.value.draftName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Household name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            householdRepository.rename(name).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "Household renamed") }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Rename failed")
                    }
                },
            )
        }
    }

    fun sendInvite() {
        val email = _uiState.value.inviteEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(error = "Enter a valid email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            householdRepository.invite(email, _uiState.value.inviteRole).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(saving = false, inviteEmail = "", message = "Invite sent to $email")
                    }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Invite failed")
                    }
                },
            )
        }
    }

    fun switchHousehold(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, message = null) }
            householdRepository.switchTo(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "Switched household") }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(saving = false, error = err.message ?: "Switch failed")
                    }
                },
            )
        }
    }

    companion object {
        fun factory(householdRepository: HouseholdRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HouseholdViewModel(householdRepository) as T
            }
    }
}
