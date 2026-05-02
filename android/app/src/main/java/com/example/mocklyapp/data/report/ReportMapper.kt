package com.example.mocklyapp.data.report

import com.example.mocklyapp.data.report.remote.ReportDto
import com.example.mocklyapp.domain.report.model.InterviewReport

object ReportMapper {

    fun toDomain(dto: ReportDto): InterviewReport {
        return InterviewReport(
            id = dto.id,
            sessionId = dto.sessionId,
            status = dto.status.uppercase(),
            summary = dto.summary,
            recommendations = dto.recommendations,
            metrics = dto.metrics,
            errorMessage = dto.errorMessage,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}