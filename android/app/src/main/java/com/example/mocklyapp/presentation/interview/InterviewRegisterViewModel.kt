package com.example.mocklyapp.presentation.interview

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
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

    fun setSelectedTime(int: Int) = _state.update { it.copy(selectedTimeIndex = int) }
    fun setAgree(v: Boolean) = _state.update { it.copy(isAgree = v) }


    @RequiresApi(Build.VERSION_CODES.O)
    fun register() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                val index = state.value.selectedTimeIndex
                    ?: return@launch _state.update {
                        it.copy(error = "Please select time", isLoading = false)
                    }

                val today = LocalDate.now()
                val (date, time) = when (index) {
                    0 -> today             to LocalTime.of(15, 0)
                    1 -> today             to LocalTime.of(17, 0)
                    2 -> today.plusDays(1) to LocalTime.of(15, 0)
                    else -> today.plusDays(1) to LocalTime.of(17, 0)
                }

                val iso = ZonedDateTime.of(date, time, ZoneId.systemDefault())
                    .toInstant().toString()

                val session = sessionRepo.createSession(
                    interviewerId = interviewerId,
                    scheduledAt = iso
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdSessionId = session.id
                    )
                }

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                val backendMessage = try {
                    JSONObject(body ?: "").optString("message")
                } catch (_: Exception) {
                    null
                }

                val displayMessage =
                    if (backendMessage?.contains("active session", ignoreCase = true) == true) {
                        "You are already registered for an interview.\nPlease complete or cancel your current session before booking a new one."
                    } else {
                        backendMessage ?: "HTTP ${e.code()} error"
                    }

                _state.update {
                    it.copy(
                        isLoading = false,
                        error = displayMessage
                    )
                }
            }
        }
    }

}
