package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.ReportApi
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.domain.report.model.InterviewReport

class ReportRepositoryImpl(
    private val reportApi: ReportApi
) : ReportRepository {

    override suspend fun getSessionReport(sessionId: String): InterviewReport {
        return reportApi.getSessionReport(sessionId).toDomain(sessionId)
    }

    override suspend fun triggerSessionReport(sessionId: String): InterviewReport {
        return reportApi.triggerSessionReport(sessionId).toDomain(sessionId)
    }
}