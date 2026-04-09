package com.example.mocklyapp.models

data class MessageItem (
    val author: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int
)