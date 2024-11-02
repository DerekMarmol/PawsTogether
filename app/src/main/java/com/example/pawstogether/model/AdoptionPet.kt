package com.example.pawstogether.model

data class AdoptionPet(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val age: Int = 0,
    val description: String = "",
    val imageUrl: String = "",
    val isNeutered: Boolean = false,
    val hasVaccines: Boolean = false,
    val medicalHistoryUrl: String? = null,
    val status: String = "available", // Nuevo campo
    val adopterId: String? = null     // Nuevo campo
)