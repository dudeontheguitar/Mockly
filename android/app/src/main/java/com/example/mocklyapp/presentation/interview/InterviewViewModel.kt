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
    val name: String = ""
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
                _state.update {
                    it.copy(name = user.name.ifBlank { user.displayName })
                }
            } catch (_: Exception) {
                // Не ломаем экран, если имя не загрузилось.
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

    fun refresh() {
        loadSessions()
    }

    private fun parseInstant(value: String?): Instant? {
        return try {
            value?.let(Instant::parse)
        } catch (_: Exception) {
            null
        }
    }
}