package com.example.mocklyapp.data.user.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String>?
)

data class UpdateUserRequest(
    val displayName: String?,
    val avatarUrl: String?,
    val level: String?
)

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): UserDto

    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") id: String
    ): UserDto

    @PATCH("users/me")
    suspend fun updateMe(
        @Body body: UpdateUserRequest
    ): UserDto
}