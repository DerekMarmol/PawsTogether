package com.example.pawstogether.ui.theme.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class DrawerItems(
    val icon: ImageVector,
    val title: String,
    val route: String
) {
    PROFILE(Icons.Default.Person, "Perfil", "profile"),
    HOME(Icons.Default.Home, "Home", "home"),
    REPORTS(Icons.Default.Report, "Reportes", "reports"),
    ADOPTION(Icons.Default.Pets, "Adopción", "adoption"),
    SERVICES(Icons.Default.Favorite, "Ofrecer Servicios", "services"),
    PET_CARE(Icons.Default.LocalHospital, "Cuidado de Mascotas", "PetCare"),
    MESSAGES(Icons.Default.Chat, "Mensajes", "chats_list")
}