package com.example.mocklyapp.domain.user

import com.example.mocklyapp.domain.user.model.User

interface UserRepository {

    suspend fun getCurrentUser(): User

    suspend fun getUserById(id: String): User

    suspend fun updateCurrentUser(
        name: String?,
        surname: String?,
        avatarUrl: String?,
        level: String?
    ): User
}