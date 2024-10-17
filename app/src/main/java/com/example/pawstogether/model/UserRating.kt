package com.example.pawstogether.model

data class UserRating(
    val fromUserId: String,
    val toUserId: String,
    val stars: Int,
    val review: String,
    val isThankYou: Boolean,
    val serviceType: String,
    val timestamp: Long = System.currentTimeMillis()
)
