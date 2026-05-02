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
        return api.getMe().toDomain()
    }

    override suspend fun getUserById(id: String): User {
        return api.getUserById(id).toDomain()
    }

    override suspend fun updateCurrentUser(
        name: String?,
        surname: String?,
        avatarUrl: String?,
        level: String?,
        skills: List<String>,
        bio: String?,
        location: String?
    ): User {
        val displayName = buildDisplayName(name, surname)

        val dto = api.updateMe(
            UpdateUserRequest(
                displayName = displayName,
                avatarUrl = avatarUrl,
                level = level?.trim()?.takeIf { it.isNotBlank() },
                skills = skills.map { it.trim() }.filter { it.isNotBlank() },
                bio = bio?.trim()?.takeIf { it.isNotBlank() },
                location = location?.trim()?.takeIf { it.isNotBlank() }
            )
        )

        return dto.toDomain()
    }

    private fun buildDisplayName(
        name: String?,
        surname: String?
    ): String? {
        val result = listOfNotNull(
            name?.trim()?.takeIf { it.isNotBlank() },
            surname?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        return result.ifBlank { null }
    }
}

private fun UserDto.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        role = role,
        avatarUrl = avatarUrl,
        level = level,
        skills = skills.orEmpty(),
        bio = bio,
        location = location
    )
}