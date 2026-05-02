package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.ReportApi
import com.example.mocklyapp.data.report.remote.ReportDto
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.domain.report.model.InterviewReport

class ReportRepositoryImpl(
    private val reportApi: ReportApi
) : ReportRepository {

    override suspend fun getSessionReport(sessionId: String): InterviewReport {
        return reportApi.getSessionReport(sessionId).toDomain()
    }
}

private fun ReportDto.toDomain(): InterviewReport {
    return InterviewReport(
        id = id,
        sessionId = sessionId,
        status = status.uppercase(),
        summary = summary,
        recommendations = recommendations,
        metrics = metrics,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}