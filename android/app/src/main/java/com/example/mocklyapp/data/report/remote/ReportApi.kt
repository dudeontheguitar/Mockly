package com.example.mocklyapp.data.report.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ReportDto(
    @SerializedName("id")
    val id: String?,

    @SerializedName("sessionId")
    val sessionId: String?,

    @SerializedName("metrics")
    val metrics: Map<String, Any>?,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("recommendations")
    val recommendations: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("errorMessage")
    val errorMessage: String?,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("updatedAt")
    val updatedAt: String?
)

interface ReportApi {

    @GET("sessions/{sessionId}/report")
    suspend fun getSessionReport(
        @Path("sessionId") sessionId: String
    ): ReportDto

    @POST("sessions/{sessionId}/report/trigger")
    suspend fun triggerSessionReport(
        @Path("sessionId") sessionId: String
    ): ReportDto
}