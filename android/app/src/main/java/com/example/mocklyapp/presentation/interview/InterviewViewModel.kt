package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.interviewslot.InterviewSlotRepository
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlotStatus
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionStatus
import com.example.mocklyapp.domain.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class InterviewUiState(
    val isLoading: Boolean = false,
    val isSlotsLoading: Boolean = false,
    val isMySlotsLoading: Boolean = false,
    val error: String? = null,
    val slotsError: String? = null,
    val mySlotsError: String? = null,
    val upcoming: List<Session> = emptyList(),
    val past: List<Session> = emptyList(),
    val availableSlots: List<InterviewSlot> = emptyList(),
    val mySlots: List<InterviewSlot> = emptyList(),
    val name: String = ""
)

class InterviewViewModel(
    private val sessionRepo: SessionRepository,
    private val userRepo: UserRepository,
    private val interviewSlotRepo: InterviewSlotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewUiState(isLoading = true))
    val state: StateFlow<InterviewUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        loadUser()
        loadSessions()
        loadAvailableSlots()
        loadMySlots()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser()
                _state.update {
                    it.copy(name = user.name.ifBlank { user.displayName })
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val all = sessionRepo.getSessions(
                    page = 0,
                    size = 100
                )

                val upcoming = all
                    .filter { session ->
                        session.status == SessionStatus.SCHEDULED ||
                                session.status == SessionStatus.ACTIVE
                    }
                    .sortedBy { session ->
                        parseInstant(session.startAt) ?: Instant.MAX
                    }

                val past = all
                    .filter { session ->
                        session.status == SessionStatus.ENDED ||
                                session.status == SessionStatus.CANCELED
                    }
                    .sortedByDescending { session ->
                        parseInstant(session.endsAt)
                            ?: parseInstant(session.startAt)
                            ?: Instant.EPOCH
                    }

                _state.update {
                    it.copy(
                        isLoading = false,
                        upcoming = upcoming,
                        past = past,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sessions."
                    )
                }
            }
        }
    }

    fun loadAvailableSlots() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSlotsLoading = true,
                    slotsError = null
                )
            }

            try {
                val slots = interviewSlotRepo.getOpenSlots()
                    .filter { slot -> slot.status == InterviewSlotStatus.OPEN }
                    .sortedBy { slot -> parseInstant(slot.scheduledAt) ?: Instant.MAX }

                _state.update {
                    it.copy(
                        isSlotsLoading = false,
                        availableSlots = slots,
                        slotsError = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSlotsLoading = false,
                        availableSlots = emptyList(),
                        slotsError = e.message ?: "Failed to load interview slots."
                    )
                }
            }
        }
    }

    fun loadMySlots() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isMySlotsLoading = true,
                    mySlotsError = null
                )
            }

            try {
                val slots = interviewSlotRepo.getMySlots()
                    .filter { slot ->
                        slot.status == InterviewSlotStatus.OPEN && slot.sessionId == null
                    }
                    .sortedBy { slot -> parseInstant(slot.scheduledAt) ?: Instant.MAX }

                _state.update {
                    it.copy(
                        isMySlotsLoading = false,
                        mySlots = slots,
                        mySlotsError = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isMySlotsLoading = false,
                        mySlots = emptyList(),
                        mySlotsError = e.message ?: "Failed to load your slots."
                    )
                }
            }
        }
    }

    private fun parseInstant(value: String?): Instant? {
        return try {
            value?.let(Instant::parse)
        } catch (_: Exception) {
            null
        }
    }
}