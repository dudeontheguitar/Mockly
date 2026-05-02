package com.example.mocklyapp.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.interviewslot.InterviewSlotRepository
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlotStatus
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.domain.report.model.InterviewReport
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionStatus
import com.example.mocklyapp.domain.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val userRole: String = "",
    val nextSession: Session? = null,
    val openSlotsCount: Int = 0,
    val lastEndedSession: Session? = null,
    val lastReport: InterviewReport? = null
)

class DiscoverViewModel(
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository,
    private val interviewSlotRepo: InterviewSlotRepository,
    private val reportRepo: ReportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState(isLoading = true))
    val state: StateFlow<DiscoverUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val user = userRepo.getCurrentUser()

                val sessions = sessionRepo.getSessions(
                    page = 0,
                    size = 100
                )

                val nextSession = sessions
                    .filter { session ->
                        session.status == SessionStatus.SCHEDULED ||
                                session.status == SessionStatus.ACTIVE
                    }
                    .sortedBy { session ->
                        parseInstant(session.startAt) ?: Instant.MAX
                    }
                    .firstOrNull()

                val lastEndedSession = sessions
                    .filter { session ->
                        session.status == SessionStatus.ENDED
                    }
                    .sortedByDescending { session ->
                        parseInstant(session.endsAt)
                            ?: parseInstant(session.startAt)
                            ?: Instant.EPOCH
                    }
                    .firstOrNull()

                val openSlotsCount = loadOpenSlotsCountSafely()

                val lastReport = if (lastEndedSession != null) {
                    loadReportSafely(lastEndedSession.id)
                } else {
                    null
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        userName = user.name.ifBlank { user.displayName },
                        userRole = user.role,
                        nextSession = nextSession,
                        openSlotsCount = openSlotsCount,
                        lastEndedSession = lastEndedSession,
                        lastReport = lastReport,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load dashboard."
                    )
                }
            }
        }
    }

    private suspend fun loadOpenSlotsCountSafely(): Int {
        return try {
            interviewSlotRepo.getOpenSlots()
                .count { slot ->
                    slot.status == InterviewSlotStatus.OPEN && slot.sessionId == null
                }
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun loadReportSafely(sessionId: String): InterviewReport? {
        return try {
            reportRepo.getSessionReport(sessionId)
        } catch (e: HttpException) {
            null
        } catch (_: Exception) {
            null
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