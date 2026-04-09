package com.example.mocklyapp.domain.auth

import com.example.mocklyapp.domain.auth.model.Tokens

interface AuthRepository {

    suspend fun login(email: String, password: String): Tokens

    suspend fun register(
        email: String,
        password: String,
        name: String,
        surname: String,
        role: String
    ): Tokens

    suspend fun refresh(refreshTokens: String): Tokens

    suspend fun logout()

    suspend fun changePassword(currentPassword: String, newPassword: String)
}
