package com.example.pawstogether.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pawstogether.model.UserRating
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RatingScreen(
    toUserId: String,
    serviceType: String,
    onRatingSubmit: (UserRating) -> Unit,
    onClose: () -> Unit
) {
    var stars by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    var isThankYou by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Califica tu experiencia",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Estrellas de calificación
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
                        tint = if (index < stars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = review,
            onValueChange = { review = it },
            label = { Text("Escribe tu reseña detallada") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isThankYou,
                onCheckedChange = { isThankYou = it }
            )
            Text("Marcar como agradecimiento especial")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val userRating = UserRating(
                    fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    toUserId = toUserId,
                    stars = stars,
                    review = review,
                    isThankYou = isThankYou,
                    serviceType = serviceType
                )
                onRatingSubmit(userRating)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = stars > 0 && review.isNotEmpty()
        ) {
            Text("Enviar Calificación")
        }
    }
}