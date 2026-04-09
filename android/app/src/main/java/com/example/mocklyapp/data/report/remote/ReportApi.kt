
package com.example.mocklyapp.data.report.remote

import com.example.mocklyapp.presentation.interview.InterviewReport
import retrofit2.http.GET
import retrofit2.http.Path

import com.google.gson.annotations.SerializedName

/**
 * Data классы для десериализации JSON ответа от backend
 * Соответствуют структуре EvaluateResponse из ML сервиса
 */

/**
 * Основной отчет о результатах интервью
 */
data class InterviewReportDto(
    @SerializedName("overallScore")
    val overallScore: Double,

    @SerializedName("overallLabel")
    val overallLabel: String,

    @SerializedName("overallMessage")
    val overallMessage: String,

    @SerializedName("strengths")
    val strengths: List<String>,

    @SerializedName("areasToImprove")
    val areasToImprove: List<String>,

    @SerializedName("speechAnalysis")
    val speechAnalysis: SpeechAnalysisDto?,

    @SerializedName("summary")
    val summary: String,

    @SerializedName("recommendations")
    val recommendations: String,

    @SerializedName("scores")
    val scores: ScoresDto?,

    @SerializedName("transcript")
    val transcript: String?
)

/**
 * Анализ речи (темп, filler words)
 */
data class SpeechAnalysisDto(
    @SerializedName("paceLabel")
    val paceLabel: String,

    @SerializedName("paceScore")
    val paceScore: Double?,

    @SerializedName("fillerWordsCount")
    val fillerWordsCount: Int,

    @SerializedName("fillerWordRate")
    val fillerWordRate: Double
)

/**
 * Детальные оценки по категориям
 */
data class ScoresDto(
    @SerializedName("communication")
    val communication: Double,

    @SerializedName("technical")
    val technical: Double,

    @SerializedName("confidence")
    val confidence: Double
)
/**
 * Retrofit API для получения отчетов о результатах интервью
 */
interface ReportApi {

    /**
     * Получить отчет ML анализа для сессии
     * @param sessionId ID сессии интервью
     * @return InterviewReportDto с результатами ML анализа
     */
    @GET("api/sessions/{sessionId}/report")
    suspend fun getSessionReport(
        @Path("sessionId") sessionId: String
    ): InterviewReportDto
}