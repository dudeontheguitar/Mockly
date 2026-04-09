package com.example.mocklyapp.data.session.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

data class SessionsPageDto(
    val sessions: List<SessionDto>
)

data class SessionDto(
    val id: String,
    val createdBy: String,
    val creatorDisplayName: String?,
    val status: String,

    @SerializedName("startsAt")
    val startAt: String?,

    @SerializedName("endsAt")
    val endsAt: String?,

    val roomProvider: String?,
    val roomId: String?,
    val recordingId: String?,
    val participants: List<SessionParticipantDto>,
    val artifacts: List<SessionArtifactDto>
)

data class SessionParticipantDto(
    val id: String,
    val userId: String,
    val userDisplayName: String?,
    val userEmail: String?,
    val roleInSession: String,
    val joinedAt: String?,
    val leftAt: String?
)

data class SessionArtifactDto(
    val id: String,
    val type: String,
    val storageUrl: String?,
    val durationSec: Int?,
    val sizeBytes: Long?
)

data class CreateSessionRequestDto(
    val interviewerId: String,
    val scheduledAt: String
)

data class LiveKitTokenDto(
    val token: String,
    val roomId: String,
    val url: String
)

interface SessionApi {

    @GET("sessions")
    suspend fun getSessions(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("status") status: String? = null
    ): SessionsPageDto

    @POST("sessions")
    suspend fun createSession(
        @Body body: CreateSessionRequestDto
    ): SessionDto

    @GET("sessions/{id}")
    suspend fun getSessionById(
        @Path("id") id: String
    ): SessionDto

    @POST("sessions/{id}/end")
    suspend fun endSession(
        @Path("id") id: String
    )

    @POST("sessions/{id}/join")
    suspend fun joinSession(
        @Path("id") id: String
    ): SessionDto

    @POST("sessions/{id}/leave")
    suspend fun leaveSession(
        @Path("id") id: String
    )

    @GET("sessions/{id}/token")
    suspend fun getSessionToken(
        @Path("id") id: String
    ): LiveKitTokenDto

    @GET("sessions/me/active")
    suspend fun getMyActiveSession(): SessionDto?
}
