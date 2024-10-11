package com.example.pawstogether.model

data class PetPost(
    val id: String = "", // Firestore necesita un constructor sin argumentos, así que proporcionamos valores por defecto
    val userId: String = "",
    val mediaUrl: String = "",
    val description: String = "",
    val isVideo: Boolean = false,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val userId: String = "",
    val text: String = ""
)

