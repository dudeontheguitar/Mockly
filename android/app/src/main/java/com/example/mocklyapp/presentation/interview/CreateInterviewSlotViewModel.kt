package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.interviewslot.InterviewSlotRepository
import com.example.mocklyapp.domain.interviewslot.model.CreateInterviewSlotRequest
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

data class CreateInterviewSlotUiState(
    val title: String = "",
    val company: String = "",
    val location: String = "Remote",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val durationMinutes: String = "30",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class CreateInterviewSlotViewModel(
    private val interviewSlotRepo: InterviewSlotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        CreateInterviewSlotUiState(
            title = "Backend Developer",
            company = "Mockly",
            location = "Remote",
            description = "Java Spring Boot mock interview",
            date = LocalDate.now().plusDays(1).toString(),
            time = "15:00",
            durationMinutes = "30"
        )
    )

    val state: StateFlow<CreateInterviewSlotUiState> = _state

    fun setTitle(value: String) {
        _state.update { it.copy(title = value, error = null) }
    }

    fun setCompany(value: String) {
        _state.update { it.copy(company = value, error = null) }
    }

    fun setLocation(value: String) {
        _state.update { it.copy(location = value, error = null) }
    }

    fun setDescription(value: String) {
        _state.update { it.copy(description = value, error = null) }
    }

    fun setDate(value: String) {
        _state.update { it.copy(date = value, error = null) }
    }

    fun setTime(value: String) {
        _state.update { it.copy(time = value, error = null) }
    }

    fun setDurationMinutes(value: String) {
        _state.update { it.copy(durationMinutes = value.filter { char -> char.isDigit() }, error = null) }
    }

    fun createSlot() {
        val current = _state.value

        val title = current.title.trim()
        val company = current.company.trim()
        val location = current.location.trim()
        val description = current.description.trim()
        val duration = current.durationMinutes.toIntOrNull()

        if (title.isBlank()) {
            setError("Title is required.")
            return
        }

        if (company.isBlank()) {
            setError("Company is required.")
            return
        }

        if (duration == null || duration <= 0) {
            setError("Duration must be a positive number.")
            return
        }

        val scheduledAt = try {
            buildScheduledAt(
                date = current.date.trim(),
                time = current.time.trim()
            )
        } catch (_: Exception) {
            setError("Date/time format must be yyyy-MM-dd and HH:mm.")
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isSuccess = false,
                    error = null
                )
            }

            try {
                interviewSlotRepo.createSlot(
                    CreateInterviewSlotRequest(
                        title = title,
                        company = company,
                        location = location,
                        description = description,
                        scheduledAt = scheduledAt,
                        durationMinutes = duration
                    )
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
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

    private fun setError(message: String) {
        _state.update {
            it.copy(
                isLoading = false,
                isSuccess = false,
                error = message
            )
        }
    }

    private fun buildScheduledAt(date: String, time: String): String {
        val localDate = LocalDate.parse(date)
        val localTime = LocalTime.parse(time)

        return ZonedDateTime
            .of(localDate, localTime, ZoneId.systemDefault())
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

                if (!backendMessage.isNullOrBlank()) {
                    backendMessage
                } else {
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
                e.message ?: "Failed to create interview slot."
        }
    }
}