package com.example.pawstogether.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class ProfileViewModel : ViewModel() {
    private val _displayName = mutableStateOf("")
    val displayName: String by _displayName

    private val _username = mutableStateOf("")
    val username: String by _username

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    init {
        refreshUserData()
    }

    fun refreshUserData() {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        _displayName.value = document.getString("displayName") ?: ""
                        _username.value = document.getString("username") ?: ""
                    }
                }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        currentUser?.uid?.let { uid ->
            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "username" to username
            )

            firestore.collection("users").document(uid)
                .update(userUpdates)
                .addOnSuccessListener {
                    _displayName.value = displayName
                    _username.value = username
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onError("Error al actualizar perfil: ${exception.message}")
                }
        }
    }
}