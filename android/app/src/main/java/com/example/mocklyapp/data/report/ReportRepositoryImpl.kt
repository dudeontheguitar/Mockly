package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.ReportApi
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.presentation.interview.InterviewReport
/**
 * Реализация ReportRepository для получения отчетов с сервера
 */
class ReportRepositoryImpl(
    private val reportApi: ReportApi
) : ReportRepository {

    /**
     * Получает отчет о результатах интервью с backend
     *
     * @param sessionId ID сессии интервью
     * @return InterviewReport с ML анализом (score, strengths, areas to improve и т.д.)
     * @throws retrofit2.HttpException если сервер вернул ошибку (404, 500 и т.д.)
     * @throws java.io.IOException если проблемы с сетью
     */
    override suspend fun getSessionReport(sessionId: String): InterviewReport {
        val dto = reportApi.getSessionReport(sessionId)
        return ReportMapper.toDomain(dto)
    }
}