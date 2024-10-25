package com.example.pawstogether.utils

import com.google.firebase.firestore.FirebaseFirestore

object Utils {
    fun getCurrentUserName(userId: String, callback: (String) -> Unit) {
        if (userId.isBlank()) {
            callback("") // O maneja el error según corresponda
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userName = document.getString("userName") ?: ""
                callback(userName)
            }
    }

}