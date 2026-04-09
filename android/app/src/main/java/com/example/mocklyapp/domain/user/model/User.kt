// domain/user/model/User.kt
package com.example.mocklyapp.domain.user.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val surname: String?,
    val role: String,
    val avatarUrl: String?,
    val level: String?,
    val skills: List<String>?
)
