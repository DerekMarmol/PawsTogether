package com.example.pawstogether.model

sealed class PostAction {
    data class Like(val postId: String) : PostAction()
    data class Unlike(val postId: String) : PostAction()
    data class Comment(val postId: String, val text: String, val userName: String) : PostAction()
}