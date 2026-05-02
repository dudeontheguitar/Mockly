package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class InterviewRegisterUiState(
    val selectedTimeIndex: Int? = null,
    val isAgree: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val createdSessionId: String? = null
)

class InterviewRegisterViewModel(
    private val sessionRepo: SessionRepository,
    private val interviewerId: String,
    val interviewerName: String,
    val jobTitle: String,
    val company: String
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewRegisterUiState())
    val state: StateFlow<InterviewRegisterUiState> = _state

    fun setSelectedTime(index: Int) {
        _state.update {
            it.copy(
                selectedTimeIndex = index,
                error = null
            )
        }
    }

    fun setAgree(value: Boolean) {
        _state.update {
            it.copy(
                isAgree = value,
                error = null
            )
        }
    }

    fun register() {
        val currentState = _state.value

        val selectedIndex = currentState.selectedTimeIndex
        if (selectedIndex == null) {
            _state.update {
                it.copy(
                    error = "Please select time",
                    isLoading = false
                )
            }
            return
        }

        if (!currentState.isAgree) {
            _state.update {
                it.copy(
                    error = "Please agree to the interview guidelines",
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(
                        isLoading = true,
                        isSuccess = false,
                        error = null,
                        createdSessionId = null
                    )
                }

                val scheduledAt = buildScheduledAt(selectedIndex)

                val session = sessionRepo.createSession(
                    interviewerId = interviewerId,
                    scheduledAt = scheduledAt
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdSessionId = session.id,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = mapError(e)
                    )
                }
            }
        }
    }

    private fun buildScheduledAt(index: Int): String {
        val today = LocalDate.now()

        val date: LocalDate
        val time: LocalTime

        when (index) {
            0 -> {
                date = today
                time = LocalTime.of(15, 0)
            }

            1 -> {
                date = today
                time = LocalTime.of(17, 0)
            }

            2 -> {
                date = today.plusDays(1)
                time = LocalTime.of(15, 0)
            }

            else -> {
                date = today.plusDays(1)
                time = LocalTime.of(17, 0)
            }
        }

        return ZonedDateTime
            .of(date, time, ZoneId.systemDefault())
            .toInstant()
            .toString()
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is HttpException -> {
                val backendMessage = try {
                    val body = e.response()?.errorBody()?.string()
                    JSONObject(body ?: "").optString("message")
                } catch (_: Exception) {
                    null
                }

                when {
                    backendMessage?.contains("active session", ignoreCase = true) == true ->
                        "You are already registered for an interview.\nPlease complete or cancel your current session before booking a new one."

                    !backendMessage.isNullOrBlank() ->
                        backendMessage

                    else ->
                        "Server error HTTP ${e.code()}."
                }
            }

            is UnknownHostException ->
                "Cannot reach the server. Please check your connection."

            is SocketTimeoutException ->
                "The server took too long to respond. Try again."

            is IOException ->
                "Network error. Please check your connection."

            else ->
                e.message ?: "Failed to register for interview."
        }
    }
}