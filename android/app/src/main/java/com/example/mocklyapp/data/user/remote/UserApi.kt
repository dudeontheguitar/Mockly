
package com.example.mocklyapp.data.user.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val surname: String?,
    val role: String,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String>?
)

data class UpdateUserRequest(
    val name: String?,
    val surname: String?,
    val avatarUrl: String?,
    val level: String?
)

interface UserApi {
    @GET("users/me")
    suspend fun getMe(): UserDto

    @PATCH("users/me")
    suspend fun updateMe(@Body body: UpdateUserRequest): UserDto
}
