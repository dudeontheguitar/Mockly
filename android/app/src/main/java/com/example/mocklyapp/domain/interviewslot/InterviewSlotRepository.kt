package com.example.mocklyapp.domain.interviewslot

import com.example.mocklyapp.domain.interviewslot.model.BookInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.CancelInterviewSlotResult
import com.example.mocklyapp.domain.interviewslot.model.CreateInterviewSlotRequest
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot

interface InterviewSlotRepository {

    suspend fun getOpenSlots(): List<InterviewSlot>

    suspend fun getMySlots(): List<InterviewSlot>

    suspend fun getSlotById(slotId: String): InterviewSlot

    suspend fun createSlot(request: CreateInterviewSlotRequest): InterviewSlot

    suspend fun bookSlot(slotId: String): BookInterviewSlotResult

    suspend fun cancelSlot(slotId: String): CancelInterviewSlotResult
}