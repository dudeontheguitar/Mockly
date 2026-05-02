package com.example.mocklyapp.data.interviewslot.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class InterviewSlotsPageDto(
    val items: List<InterviewSlotDto>? = emptyList()
)

data class InterviewSlotDto(
    val id: String,
    val title: String?,
    val company: String?,
    val location: String?,
    val description: String?,
    val scheduledAt: String?,
    val durationMinutes: Int?,
    val status: String?,
    val interviewer: InterviewSlotUserDto?,
    val bookedBy: InterviewSlotUserDto?,
    val sessionId: String?
)

data class InterviewSlotUserDto(
    val id: String,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String>?,
    val bio: String?
)

data class CreateInterviewSlotRequestDto(
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val scheduledAt: String,
    val durationMinutes: Int
)

data class BookInterviewSlotResponseDto(
    val slotId: String,
    val sessionId: String,
    val status: String
)

data class CancelInterviewSlotResponseDto(
    val slotId: String,
    val status: String
)

interface InterviewSlotApi {

    @GET("interview-slots")
    suspend fun getOpenSlots(): InterviewSlotsPageDto

    @POST("interview-slots")
    suspend fun createSlot(
        @Body body: CreateInterviewSlotRequestDto
    ): InterviewSlotDto

    @GET("interview-slots/my")
    suspend fun getMySlots(): InterviewSlotsPageDto

    @GET("interview-slots/{slotId}")
    suspend fun getSlotById(
        @Path("slotId") slotId: String
    ): InterviewSlotDto

    @POST("interview-slots/{slotId}/book")
    suspend fun bookSlot(
        @Path("slotId") slotId: String
    ): BookInterviewSlotResponseDto

    @POST("interview-slots/{slotId}/cancel")
    suspend fun cancelSlot(
        @Path("slotId") slotId: String
    ): CancelInterviewSlotResponseDto
}