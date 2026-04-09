package com.example.mocklyapp.domain.auth.model

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)