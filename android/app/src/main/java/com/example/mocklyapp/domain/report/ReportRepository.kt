package com.example.mocklyapp.domain.report

import com.example.mocklyapp.presentation.interview.InterviewReport



/**
 * Repository интерфейс для работы с отчетами интервью
 */
interface ReportRepository {

    /**
     * Получить отчет о результатах интервью
     * @param sessionId ID сессии интервью
     * @return InterviewReport с детальными результатами ML анализа
     * @throws Exception если отчет не найден или произошла ошибка
     */
    suspend fun getSessionReport(sessionId: String): InterviewReport
}