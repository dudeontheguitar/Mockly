package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.InterviewReportDto
import com.example.mocklyapp.data.report.remote.ScoresDto
import com.example.mocklyapp.data.report.remote.SpeechAnalysisDto
import com.example.mocklyapp.presentation.interview.InterviewReport
import com.example.mocklyapp.presentation.interview.Scores
import com.example.mocklyapp.presentation.interview.SpeechAnalysis

/**
 * Маппер для конвертации DTO в domain модели
 */
object ReportMapper {

    /**
     * Конвертирует InterviewReportDto (из API) в InterviewReport (domain)
     */
    fun toDomain(dto: InterviewReportDto): InterviewReport {
        return InterviewReport(
            overallScore = dto.overallScore,
            overallLabel = dto.overallLabel,
            overallMessage = dto.overallMessage,
            strengths = dto.strengths,
            areasToImprove = dto.areasToImprove,
            speechAnalysis = dto.speechAnalysis?.toDomain(),
            scores = dto.scores?.toDomain(),
            summary = dto.summary,
            recommendations = dto.recommendations,
            transcript = dto.transcript
        )
    }

    /**
     * Конвертирует SpeechAnalysisDto в SpeechAnalysis
     */
    private fun SpeechAnalysisDto.toDomain(): SpeechAnalysis {
        return SpeechAnalysis(
            paceLabel = paceLabel,
            paceScore = paceScore,
            fillerWordsCount = fillerWordsCount,
            fillerWordRate = fillerWordRate
        )
    }

    /**
     * Конвертирует ScoresDto в Scores
     */
    private fun ScoresDto.toDomain(): Scores {
        return Scores(
            communication = communication,
            technical = technical,
            confidence = confidence
        )
    }
}