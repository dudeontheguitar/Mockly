package com.example.mocklyapp.domain.session

import com.example.mocklyapp.domain.session.model.*

interface SessionRepository {

    suspend fun getSessions(
        status: SessionStatus? = null,
        page: Int? = null,
        size: Int? = null
    ): List<Session>

    suspend fun getActiveSession(): Session?

    suspend fun createSession(
        interviewerId: String,
        scheduledAt: String
    ): Session

    suspend fun getSessionById(id: String): Session

    suspend fun joinSession(id: String): Session

    suspend fun leaveSession(id: String)

    suspend fun endSession(id: String)

    suspend fun getLiveKitToken(id: String): LiveKitToken
}
