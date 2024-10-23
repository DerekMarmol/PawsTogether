package com.example.pawstogether.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pawstogether.model.UserRating
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RatingScreen(
    userName: String,
    serviceType: String,
    toUserId: String,
    onRatingSubmit: (UserRating) -> Unit,
    onClose: () -> Unit
) {
    var stars by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    var isThankYou by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf(serviceType) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado
        Text(
            text = "Calificar a $userName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tipo de servicio
        Text(
            text = "Servicio: $serviceType",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Estrellas de calificación
        Text(
            text = "¿Cómo calificarías tu experiencia?",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) { index ->
                IconButton(
                    onClick = { stars = index + 1 }
                ) {
                    Icon(
                        imageVector = if (index < stars) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Estrella ${index + 1}",
                        tint = if (index < stars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Mensaje según la calificación
        Text(
            text = when (stars) {
                0 -> ""
                1, 2 -> "¿Qué podría haber mejorado?"
                3 -> "Fue una experiencia aceptable"
                4 -> "¡Fue una buena experiencia!"
                5 -> "¡Excelente experiencia!"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de reseña
        OutlinedTextField(
            value = review,
            onValueChange = { review = it },
            label = { Text("Comparte tu experiencia detallada") },
            placeholder = { Text("Describe cómo fue tu experiencia con ${userName}...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Opción de agradecimiento especial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isThankYou,
                onCheckedChange = { isThankYou = it }
            )
            Text(
                text = "Marcar como agradecimiento especial",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isThankYou) {
            Text(
                text = "Tu agradecimiento especial será destacado en el perfil de $userName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val userRating = UserRating(
                        fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        toUserId = toUserId,
                        stars = stars,
                        review = review,
                        isThankYou = isThankYou,
                        serviceType = selectedService,
                        timestamp = System.currentTimeMillis()
                    )
                    onRatingSubmit(userRating)
                },
                modifier = Modifier.weight(1f),
                enabled = stars > 0 && review.isNotEmpty()
            ) {
                Text("Enviar Calificación")
            }
        }
    }
}