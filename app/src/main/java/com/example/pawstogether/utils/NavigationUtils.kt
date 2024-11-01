package com.example.pawstogether.utils

import androidx.navigation.NavController
import com.example.pawstogether.navigation.NavigationRoutes

fun NavController.navigateToChat(userId: String, userName: String) {
    this.navigate(NavigationRoutes.createChatRoute(userId, userName))
}
