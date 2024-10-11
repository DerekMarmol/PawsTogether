package com.example.pawstogether.model

data class PetPost(
    val id: String,
    val userId: String,
    val mediaUrl: String,
    val description: String,
    val isVideo: Boolean,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(), // Lista de usuarios que dieron like
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val userId: String,
    val text: String
)


