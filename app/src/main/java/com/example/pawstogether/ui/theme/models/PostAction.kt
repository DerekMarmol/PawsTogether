package com.example.pawstogether.ui.theme.models

sealed class PostAction {
    data class Like(val postId: String) : PostAction()
    data class Unlike(val postId: String) : PostAction()
    data class Comment(val postId: String, val text: String) : PostAction() // Aquí acepta un String
}

