package com.example.mocklyapp.domain.user.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String> = emptyList()
) {
    val name: String
        get() = displayName.trim().substringBefore(" ")

    val surname: String
        get() = displayName.trim().substringAfter(" ", "")
}