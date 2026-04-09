package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null,
    val upcoming: List<Session> = emptyList(),
    val past: List<Session> = emptyList(),
    val name: String = "",
)

class InterviewViewModel(
    private val sessionRepo: SessionRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewUiState(isLoading = true))
    val state: StateFlow<InterviewUiState> = _state

    init {
        loadUser()
        loadSessions()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser()
                _state.update { it.copy(name = user.name) }
            } catch (_: Exception) {
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val all = sessionRepo.getSessions()
                val now = Instant.now()

                fun parseInstant(value: String?): Instant? =
                    try {
                        value?.let(Instant::parse)
                    } catch (_: Exception) {
                        null
                    }

                val upcoming = all.filter { session ->
                    val end = parseInstant(session.endsAt)
                    when (session.status) {
                        SessionStatus.SCHEDULED,
                        SessionStatus.ACTIVE ->
                            end == null || end.isAfter(now)
                        else -> false
                    }
                }

                val past = all.filter { session ->
                    val end = parseInstant(session.endsAt)
                    when (session.status) {
                        SessionStatus.ENDED -> true
                        SessionStatus.SCHEDULED ->
                            end != null && end.isBefore(now)
                        else -> false
                    }
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
}
