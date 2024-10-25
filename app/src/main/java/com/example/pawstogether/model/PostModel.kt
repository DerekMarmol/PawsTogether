package com.example.pawstogether.model

data class PetPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val mediaUrl: String = "",
    val description: String = "",
    val isVideo: Boolean = false,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = 0,
)

data class PetReport(
    val id: String = "",
    val userId: String = "",
    val petType: String = "",
    val petBreed: String = "",
    val petColor: String = "",
    val petName: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val reportType: String = "",
    val mediaUrl: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val status: String = "active",
    val timestamp: Long = 0
)

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

