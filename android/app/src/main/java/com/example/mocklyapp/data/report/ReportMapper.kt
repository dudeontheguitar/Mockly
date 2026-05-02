package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.ReportDto
import com.example.mocklyapp.domain.report.model.InterviewReport

fun ReportDto.toDomain(fallbackSessionId: String): InterviewReport {
    return InterviewReport(
        id = id.orEmpty(),
        sessionId = sessionId ?: fallbackSessionId,
        status = status.orEmpty().uppercase(),
        summary = summary,
        recommendations = recommendations,
        metrics = metrics,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}