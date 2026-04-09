package com.example.mocklyapp.data.user

import com.example.mocklyapp.data.user.remote.UpdateUserRequest
import com.example.mocklyapp.data.user.remote.UserApi
import com.example.mocklyapp.data.user.remote.UserDto
import com.example.mocklyapp.domain.user.UserRepository
import com.example.mocklyapp.domain.user.model.User

class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {

    override suspend fun getCurrentUser(): User {
        val dto = api.getMe()
        return dto.toDomain()
    }

    override suspend fun getUserById(id: String): User {
        TODO("Not yet implemented")
    }

    override suspend fun updateCurrentUser(
        name: String?,
        surname: String?,
        avatarUrl: String?,
        level: String?
    ): User {
        val dto = api.updateMe(
            UpdateUserRequest(
                name = name,
                surname = surname,
                avatarUrl = avatarUrl,
                level = level
            )
        )
        return dto.toDomain()
    }
}

private fun UserDto.toDomain() = User(
    id = id,
    email = email,
    name = name,
    surname = surname,
    role = role,
    avatarUrl = avatarUrl,
    level = level,
    skills = skills ?: emptyList()
)
