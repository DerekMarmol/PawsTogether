package com.example.pawstogether.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    var displayName by mutableStateOf("")
        private set

    fun updateDisplayName(newName: String) {
        displayName = newName
    }
}