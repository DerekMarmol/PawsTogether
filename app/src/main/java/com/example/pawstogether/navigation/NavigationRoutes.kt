package com.example.pawstogether.navigation

object NavigationRoutes {
    const val CHAT_SCREEN = "chat/{userId}/{userName}"

    fun createChatRoute(userId: String, userName: String): String {
        return "chat/$userId/$userName"
    }
}