package com.example.mocklyapp.data.session

import com.example.mocklyapp.data.session.remote.CreateSessionRequestDto
import com.example.mocklyapp.data.session.remote.SessionApi
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.domain.session.model.LiveKitToken
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionStatus
import retrofit2.HttpException

class SessionRepositoryImpl(
    private val api: SessionApi
) : SessionRepository {

    override suspend fun getSessions(
        status: SessionStatus?,
        page: Int?,
        size: Int?
    ): List<Session> {
        val resp = api.getSessions(
            page = page,
            size = size,
            status = status?.name
        )

        return resp.sessions.map { it.toDomain() }
    }

    override suspend fun getActiveSession(): Session? {
        return try {
            api.getMyActiveSession().toDomain()
        } catch (e: HttpException) {
            if (e.code() == 404) {
                null
            } else {
                throw e
            }
        }
    }

    override suspend fun createSession(
        interviewerId: String,
        scheduledAt: String
    ): Session {
        val dto = api.createSession(
            CreateSessionRequestDto(
                interviewerId = interviewerId,
                scheduledAt = scheduledAt
            )
        )

        return dto.toDomain()
    }

    override suspend fun getSessionById(id: String): Session {
        return api.getSessionById(id).toDomain()
    }

    override suspend fun joinSession(id: String): Session {
        return api.joinSession(id).toDomain()
    }

    override suspend fun leaveSession(id: String) {
        api.leaveSession(id)
    }

    override suspend fun endSession(id: String) {
        api.endSession(id)
    }

    override suspend fun getLiveKitToken(id: String): LiveKitToken {
        val dto = api.getSessionToken(id)

        return LiveKitToken(
            token = dto.token,
            roomId = dto.roomId,
            url = dto.url
        )
    }
}