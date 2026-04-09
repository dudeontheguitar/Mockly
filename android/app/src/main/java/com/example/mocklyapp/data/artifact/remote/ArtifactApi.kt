package com.example.mocklyapp.data.artifact.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class RequestUploadRequestDto(
    val type: String,          // "AUDIO_MIXED"
    val fileName: String,
    val fileSizeBytes: Long,
    val contentType: String    // "audio/mpeg"
)

data class RequestUploadResponseDto(
    val artifactId: String,
    val uploadUrl: String,
    val objectName: String,
    val expiresInSeconds: Int
)

data class CompleteUploadRequestDto(
    val fileSizeBytes: Long,
    val durationSec: Int
)

data class ArtifactResponseDto(
    val id: String,
    val sessionId: String,
    val type: String,
    val storageUrl: String,
    val durationSec: Int?,
    val sizeBytes: Long?,
    val createdAt: String
)

interface ArtifactApi {

    @POST("sessions/{sessionId}/artifacts/request-upload")
    suspend fun requestUpload(
        @Path("sessionId") sessionId: String,
        @Body body: RequestUploadRequestDto
    ): RequestUploadResponseDto

    @POST("sessions/{sessionId}/artifacts/{artifactId}/complete")
    suspend fun completeUpload(
        @Path("sessionId") sessionId: String,
        @Path("artifactId") artifactId: String,
        @Body body: CompleteUploadRequestDto
    ): ArtifactResponseDto
}
