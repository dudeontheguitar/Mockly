package com.example.mocklyapp.data.session

import com.example.mocklyapp.data.session.remote.SessionArtifactDto
import com.example.mocklyapp.data.session.remote.SessionDto
import com.example.mocklyapp.data.session.remote.SessionInterviewDto
import com.example.mocklyapp.data.session.remote.SessionParticipantDto
import com.example.mocklyapp.domain.session.model.LiveKitToken
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionArtifact
import com.example.mocklyapp.domain.session.model.SessionInterview
import com.example.mocklyapp.domain.session.model.SessionParticipant
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.domain.session.model.SessionStatus

fun SessionDto.toDomain(): Session =
    Session(
        id = id,
        createdBy = createdBy,
        creatorDisplayName = creatorDisplayName,
        status = status.toSessionStatus(),
        startAt = startAt,
        endsAt = endsAt,
        roomProvider = roomProvider,
        roomId = roomId,
        recordingId = recordingId,
        interview = interview?.toDomain(),
        participants = participants.orEmpty().map { it.toDomain() },
        artifacts = artifacts.orEmpty().map { it.toDomain() }
    )

fun SessionInterviewDto.toDomain(): SessionInterview =
    SessionInterview(
        slotId = slotId,
        title = title,
        company = company,
        location = location,
        description = description,
        durationMinutes = durationMinutes
    )

fun SessionParticipantDto.toDomain(): SessionParticipant =
    SessionParticipant(
        id = id,
        userId = userId,
        userDisplayName = userDisplayName,
        userEmail = userEmail,
        userAvatarUrl = userAvatarUrl,
        roleInSession = roleInSession.toSessionRole(),
        joinedAt = joinedAt,
        leftAt = leftAt
    )

fun SessionArtifactDto.toDomain(): SessionArtifact =
    SessionArtifact(
        id = id,
        type = type,
        storageUrl = storageUrl,
        durationSec = durationSec,
        sizeBytes = sizeBytes
    )

private fun String.toSessionStatus(): SessionStatus {
    return runCatching {
        SessionStatus.valueOf(this)
    }.getOrDefault(SessionStatus.SCHEDULED)
}

private fun String.toSessionRole(): SessionRole {
    return runCatching {
        SessionRole.valueOf(this)
    }.getOrDefault(SessionRole.CANDIDATE)
}