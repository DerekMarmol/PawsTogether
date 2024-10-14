package com.example.pawstogether.model

data class PetPost(
    val id: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val description: String = "",
    val isVideo: Boolean = false,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = 0 // Nuevo campo
)

data class Comment(
    val userId: String = "",
    val text: String = ""
)

