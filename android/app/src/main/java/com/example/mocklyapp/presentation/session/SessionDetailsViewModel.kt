package com.example.mocklyapp.presentation.sessiondetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
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
    // UI-поля
    val title: String = "",
    val company: String = "",
    val interviewerName: String = "",
    val candidateName: String = "",
    val formattedTime: String = ""
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
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // пока просто берём все сессии и ищем нужную
                val all = sessionRepo.getSessions()
                val session = all.firstOrNull { it.id == sessionId }
                    ?: throw IllegalStateException("Session not found")

                val interviewer = session.participants
                    .firstOrNull { it.roleInSession == SessionRole.INTERVIEWER }
                val candidate = session.participants
                    .firstOrNull { it.roleInSession == SessionRole.CANDIDATE }

                val formattedTime = session.startAt?.let { iso ->
                    try {
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
                } ?: "-"

                _state.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        // ⚠️ ПОКА ЗАГЛУШКИ – потом подставим реальные поля из Session
                        title = "Mock Interview",
                        company = "",
                        interviewerName = interviewer?.userDisplayName ?: "Interviewer",
                        candidateName = candidate?.userDisplayName ?: "Candidate",
                        formattedTime = formattedTime,
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
}
