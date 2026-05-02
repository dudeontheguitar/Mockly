package com.example.mocklyapp.domain.report.model

data class InterviewReport(
    val id: String,
    val sessionId: String,
    val status: String, // PENDING | PROCESSING | READY | FAILED
    val summary: String?,
    val recommendations: String?,
    val metrics: Map<String, Any>?,
    val errorMessage: String?,
    val createdAt: String?,
    val updatedAt: String?
)