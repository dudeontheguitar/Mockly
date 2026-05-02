package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.interviewslot.InterviewSlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class InterviewRegisterUiState(
    val isAgree: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val createdSessionId: String? = null
)

class InterviewRegisterViewModel(
    private val interviewSlotRepo: InterviewSlotRepository,
    private val slotId: String,
    val interviewerName: String,
    val jobTitle: String,
    val company: String,
    val location: String,
    val scheduledAt: String?,
    val durationMinutes: Int
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewRegisterUiState())
    val state: StateFlow<InterviewRegisterUiState> = _state

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

                val result = interviewSlotRepo.bookSlot(slotId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdSessionId = result.sessionId,
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
                    backendMessage?.contains("already", ignoreCase = true) == true ->
                        "This interview slot is already booked."

                    backendMessage?.contains("not found", ignoreCase = true) == true ->
                        "Interview slot was not found."

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
                e.message ?: "Failed to book interview."
        }
    }
}