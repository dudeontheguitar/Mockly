package com.example.mocklyapp.presentation.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.report.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InterviewResultsState(
    val isLoading: Boolean = true,
    val report: InterviewReport? = null,
    val error: String? = null
)

data class InterviewReport(
    val overallScore: Double,
    val overallLabel: String,
    val overallMessage: String,
    val strengths: List<String>,
    val areasToImprove: List<String>,
    val speechAnalysis: SpeechAnalysis?,
    val scores: Scores?,
    val summary: String,
    val recommendations: String,
    val transcript: String?
)

data class SpeechAnalysis(
    val paceLabel: String,
    val paceScore: Double?,
    val fillerWordsCount: Int,
    val fillerWordRate: Double
)

data class Scores(
    val communication: Double,
    val technical: Double,
    val confidence: Double
)

class InterviewResultsViewModel(
    private val reportRepository: ReportRepository,
    private val sessionId: String
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewResultsState())
    val state: StateFlow<InterviewResultsState> = _state.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun loadResults() {
        viewModelScope.launch {
            _state.value = InterviewResultsState(isLoading = true)
            try {
                val report = reportRepository.getSessionReport(sessionId)
                _state.value = InterviewResultsState(
                    isLoading = false,
                    report = report
                )
                pollingJob?.cancel() // останавливаем polling когда получили результат
            } catch (e: Exception) {
                _state.value = InterviewResultsState(
                    isLoading = false,
                    error = e.message ?: "Failed to load interview results"
                )
            }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                loadResults()
                if (_state.value.report != null || _state.value.error != null) {
                    break // выходим если получили результат или ошибку
                }
                kotlinx.coroutines.delay(3000) // проверяем каждые 3 секунды
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}