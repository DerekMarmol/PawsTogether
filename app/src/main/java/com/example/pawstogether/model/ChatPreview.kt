package com.example.pawstogether.model

data class ChatPreview(
    val userId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)