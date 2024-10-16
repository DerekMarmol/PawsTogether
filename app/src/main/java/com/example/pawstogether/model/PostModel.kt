package com.example.pawstogether.model

data class PetPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "", // Add this field
    val mediaUrl: String = "",
    val description: String = "",
    val isVideo: Boolean = false,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = 0,
    val location: String = "",
    val date: String = "",
    val reportType: String = ""
)

data class Comment(
    val userId: String = "",
    val userName: String = "", // Add this field
    val text: String = ""
)

