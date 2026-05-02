package com.example.mocklyapp.domain.report

import com.example.mocklyapp.domain.report.model.InterviewReport

interface ReportRepository {

    suspend fun getSessionReport(sessionId: String): InterviewReport
}