package com.example.mocklyapp.presentation.sessiondetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.domain.session.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SessionDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: Session? = null,
    val title: String = "",
    val company: String = "",
    val interviewerName: String = "",
    val candidateName: String = "",
    val formattedTime: String = "",
    val statusText: String = "",
    val canJoin: Boolean = false
)

class SessionDetailsViewModel(
    private val sessionRepo: SessionRepository,
    private val sessionId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailsUiState(isLoading = true))
    val state: StateFlow<SessionDetailsUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val session = sessionRepo.getSessionById(sessionId)

                val interviewer = session.participants
                    .firstOrNull { it.roleInSession == SessionRole.INTERVIEWER }

                val candidate = session.participants
                    .firstOrNull { it.roleInSession == SessionRole.CANDIDATE }

                _state.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        title = "Mock Interview",
                        company = session.roomProvider ?: "LiveKit",
                        interviewerName = interviewer?.userDisplayName ?: "Interviewer",
                        candidateName = candidate?.userDisplayName ?: "Candidate",
                        formattedTime = formatTime(session.startAt),
                        statusText = session.status.name,
                        canJoin = session.status == SessionStatus.SCHEDULED ||
                                session.status == SessionStatus.ACTIVE,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load session."
                    )
                }
            }
        }
    }

    private fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"

        return try {
            val instant = Instant.parse(iso)
            val zoned = instant.atZone(ZoneId.systemDefault())

            val today = LocalDate.now()
            val dateLabel = when (zoned.toLocalDate()) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> zoned.toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }

            val timeLabel = zoned.toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a"))

            "$dateLabel, $timeLabel"
        } catch (_: Exception) {
            "-"
        }
    }
}