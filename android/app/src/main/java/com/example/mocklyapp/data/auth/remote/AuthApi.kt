// data/auth/remote/AuthApi.kt
package com.example.mocklyapp.data.auth.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val surname: String,
    val role: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

interface AuthApi{
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponseDto

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)
}
