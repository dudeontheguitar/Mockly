package com.example.mocklyapp.domain.interviewslot.model

data class InterviewSlot(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val scheduledAt: String?,
    val durationMinutes: Int,
    val status: InterviewSlotStatus,
    val interviewer: InterviewSlotUser?,
    val bookedBy: InterviewSlotUser?,
    val sessionId: String?
)

data class InterviewSlotUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String>,
    val bio: String?
)

enum class InterviewSlotStatus {
    OPEN,
    BOOKED,
    CANCELED
}

data class CreateInterviewSlotRequest(
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val scheduledAt: String,
    val durationMinutes: Int
)

data class BookInterviewSlotResult(
    val slotId: String,
    val sessionId: String,
    val status: InterviewSlotStatus
)

data class CancelInterviewSlotResult(
    val slotId: String,
    val status: InterviewSlotStatus
)