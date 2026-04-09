package com.example.mocklyapp.domain.session.model

data class Session(
    val id: String,
    val createdBy: String,
    val creatorDisplayName: String?,
    val status: SessionStatus,
    val startAt: String?,
    val endsAt: String?,
    val roomProvider: String?,
    val roomId: String?,
    val recordingId: String?,
    val participants: List<SessionParticipant>,
    val artifacts: List<SessionArtifact>
)

enum class SessionStatus {
    SCHEDULED,
    ACTIVE,
    ENDED,
    CANCELED
}

data class SessionParticipant(
    val id: String,
    val userId: String,
    val userDisplayName: String?,
    val userEmail: String?,
    val roleInSession: SessionRole,
    val joinedAt: String?,
    val leftAt: String?
)

enum class SessionRole {
    CANDIDATE,
    INTERVIEWER
}

data class SessionArtifact(
    val id: String,
    val type: String,
    val storageUrl: String?,
    val durationSec: Int?,
    val sizeBytes: Long?
)

data class LiveKitToken(
    val token: String,
    val roomId: String,
    val url: String
)
