package com.example.mocklyapp.data.interviewslot

import com.example.mocklyapp.data.interviewslot.remote.CreateInterviewSlotRequestDto
import com.example.mocklyapp.data.interviewslot.remote.InterviewSlotApi
import com.example.mocklyapp.domain.interviewslot.InterviewSlotRepository
import com.example.mocklyapp.domain.interviewslot.model.BookInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.CancelInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.CreateInterviewSlotRequest
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot

class InterviewSlotRepositoryImpl(
    private val api: InterviewSlotApi
) : InterviewSlotRepository {

    override suspend fun getOpenSlots(): List<InterviewSlot> {
        return api.getOpenSlots()
            .items
            .orEmpty()
            .map { it.toDomain() }
    }

    override suspend fun getMySlots(): List<InterviewSlot> {
        return api.getMySlots()
            .items
            .orEmpty()
            .map { it.toDomain() }
    }

    override suspend fun getSlotById(slotId: String): InterviewSlot {
        return api.getSlotById(slotId).toDomain()
    }

    override suspend fun createSlot(request: CreateInterviewSlotRequest): InterviewSlot {
        val body = CreateInterviewSlotRequestDto(
            title = request.title,
            company = request.company,
            location = request.location,
            description = request.description,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.durationMinutes
        )

        return api.createSlot(body).toDomain()
    }

    override suspend fun bookSlot(slotId: String): BookInterviewSlotResult {
        return api.bookSlot(slotId).toDomain()
    }

    override suspend fun cancelSlot(slotId: String): CancelInterviewSlotResult {
        return api.cancelSlot(slotId).toDomain()
    }
}