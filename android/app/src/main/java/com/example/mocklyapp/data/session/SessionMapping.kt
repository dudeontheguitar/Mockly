package com.example.mocklyapp.data.session

import com.example.mocklyapp.data.session.remote.SessionArtifactDto
import com.example.mocklyapp.data.session.remote.SessionDto
import com.example.mocklyapp.data.session.remote.SessionParticipantDto
import com.example.mocklyapp.domain.session.model.*

fun SessionDto.toDomain(): Session =
    Session(
        id = id,
        createdBy = createdBy,
        creatorDisplayName = creatorDisplayName,
        status = SessionStatus.valueOf(status),
        startAt = startAt,
        endsAt = endsAt,
        roomProvider = roomProvider,
        roomId = roomId,
        recordingId = recordingId,
        participants = participants.map { it.toDomain() },
        artifacts = artifacts.map { it.toDomain() }
    )

fun SessionParticipantDto.toDomain(): SessionParticipant =
    SessionParticipant(
        id = id,
        userId = userId,
        userDisplayName = userDisplayName,
        userEmail = userEmail,
        roleInSession = SessionRole.valueOf(roleInSession),
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
