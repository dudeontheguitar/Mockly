package com.example.mocklyapp.data.interviewslot

import com.example.mocklyapp.data.interviewslot.remote.BookInterviewSlotResponseDto
import com.example.mocklyapp.data.interviewslot.remote.CancelInterviewSlotResponseDto
import com.example.mocklyapp.data.interviewslot.remote.InterviewSlotDto
import com.example.mocklyapp.data.interviewslot.remote.InterviewSlotUserDto
import com.example.mocklyapp.domain.interviewslot.model.BookInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.CancelInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlotStatus
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlotUser

fun InterviewSlotDto.toDomain(): InterviewSlot =
    InterviewSlot(
        id = id,
        title = title.orEmpty(),
        company = company.orEmpty(),
        location = location.orEmpty(),
        description = description.orEmpty(),
        scheduledAt = scheduledAt,
        durationMinutes = durationMinutes ?: 30,
        status = status.toInterviewSlotStatus(),
        interviewer = interviewer?.toDomain(),
        bookedBy = bookedBy?.toDomain(),
        sessionId = sessionId
    )

fun InterviewSlotUserDto.toDomain(): InterviewSlotUser =
    InterviewSlotUser(
        id = id,
        displayName = displayName,
        email = email,
        avatarUrl = avatarUrl,
        level = level,
        skills = skills.orEmpty(),
        bio = bio
    )

fun BookInterviewSlotResponseDto.toDomain(): BookInterviewSlotResult =
    BookInterviewSlotResult(
        slotId = slotId,
        sessionId = sessionId,
        status = status.toInterviewSlotStatus()
    )

fun CancelInterviewSlotResponseDto.toDomain(): CancelInterviewSlotResult =
    CancelInterviewSlotResult(
        slotId = slotId,
        status = status.toInterviewSlotStatus()
    )

private fun String?.toInterviewSlotStatus(): InterviewSlotStatus {
    return runCatching {
        InterviewSlotStatus.valueOf(this ?: InterviewSlotStatus.OPEN.name)
    }.getOrDefault(InterviewSlotStatus.OPEN)
}