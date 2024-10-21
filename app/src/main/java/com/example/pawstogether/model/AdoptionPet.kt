package com.example.pawstogether.model

data class AdoptionPet(
    val id: String = "",
    val ownerId: String = "",
    val petName: String = "",
    val description: String = "",
    val medicalHistoryUrl: String? = null,
    val imageUrl: String = "",
    val adoptionRequests: List<String> = listOf(),
    val timestamp: Long = System.currentTimeMillis()
)
