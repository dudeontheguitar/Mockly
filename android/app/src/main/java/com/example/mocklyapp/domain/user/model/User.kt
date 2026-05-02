package com.example.mocklyapp.domain.user.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String> = emptyList(),
    val bio: String? = null,
    val location: String? = null
) {
    val name: String
        get() = displayName.trim().substringBefore(" ")

    val surname: String
        get() = displayName.trim().substringAfter(" ", "")
}