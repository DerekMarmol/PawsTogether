package com.example.pawstogether.model

import com.google.firebase.Timestamp

data class Service(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val serviceType: String = "",
    val serviceDescription: String = "",
    val serviceCost: String = "",
    val isFreeService: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null
)

data class ServiceRequest(
    val id: String = "",
    val serviceId: String = "",
    val providerId: String = "",
    val requesterId: String = "",
    val providerName: String = "",
    val requesterName: String = "",
    val serviceType: String = "",
    val status: String = "pending",
    val timestamp: Timestamp? = null,
    val isProviderConfirmed: Boolean = false,
    val isRequesterConfirmed: Boolean = false
)