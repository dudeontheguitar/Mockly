package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.domain.report.model.InterviewReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class InterviewResultsState(
    val isLoading: Boolean = true,
    val isWaitingForReport: Boolean = false,
    val report: InterviewReport? = null,
    val error: String? = null,
    val message: String? = null,
    val noReportReason: String? = null
)

class InterviewResultsViewModel(
    private val reportRepository: ReportRepository,
    private val sessionId: String,
    private val noReportReason: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewResultsState())
    val state: StateFlow<InterviewResultsState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        pollingJob?.cancel()

        if (!noReportReason.isNullOrBlank()) {
            _state.value = InterviewResultsState(
                isLoading = false,
                isWaitingForReport = false,
                report = null,
                error = null,
                message = null,
                noReportReason = noReportReason
            )
            return
        }

        pollingJob = viewModelScope.launch {
            _state.value = InterviewResultsState(
                isLoading = true,
                isWaitingForReport = true,
                message = "Preparing report..."
            )

            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                attempts++

                val shouldStop = loadResultsOnce()

                if (shouldStop) {
                    break
                }

                delay(3000)
            }

            val current = _state.value
            if (
                current.report == null &&
                current.error == null &&
                current.isWaitingForReport
            ) {
                _state.value = current.copy(
                    isLoading = false,
                    isWaitingForReport = false,
                    error = "Report is still not ready. Please try again later."
                )
            }
        }
    }

    fun retry() {
        startPolling()
    }

    private suspend fun loadResultsOnce(): Boolean {
        return try {
            val report = reportRepository.getSessionReport(sessionId)
            val status = report.status.uppercase()

            _state.value = InterviewResultsState(
                isLoading = false,
                isWaitingForReport = status == "PENDING" || status == "PROCESSING",
                report = report,
                error = if (status == "FAILED") {
                    report.errorMessage ?: "Report generation failed."
                } else {
                    null
                },
                message = when (status) {
                    "READY" -> null
                    "FAILED" -> report.errorMessage ?: "Report generation failed."
                    "PROCESSING" -> "Report is being generated..."
                    "PENDING" -> "Report is waiting for processing..."
                    else -> "Report status: $status"
                }
            )

            status == "READY" || status == "FAILED"
        } catch (e: HttpException) {
            if (e.code() == 404) {
                _state.value = InterviewResultsState(
                    isLoading = false,
                    isWaitingForReport = true,
                    report = null,
                    error = null,
                    message = getBackendMessage(e)
                        ?: "Waiting for interview recording and report generation..."
                )

                false
            } else {
                _state.value = InterviewResultsState(
                    isLoading = false,
                    isWaitingForReport = false,
                    report = null,
                    error = mapError(e),
                    message = null
                )

                true
            }
        } catch (e: Exception) {
            _state.value = InterviewResultsState(
                isLoading = false,
                isWaitingForReport = false,
                report = null,
                error = mapError(e),
                message = null
            )

            true
        }
    }

    private fun getBackendMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string()
            JSONObject(body ?: "").optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is HttpException -> {
                val backendMessage = getBackendMessage(e)
                backendMessage ?: "Server error HTTP ${e.code()}."
            }

            is UnknownHostException ->
                "Cannot reach the server. Please check your connection."

            is SocketTimeoutException ->
                "The server took too long to respond. Try again."

            is IOException ->
                "Network error. Please check your connection."

            else ->
                e.message ?: "Failed to load interview results."
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}