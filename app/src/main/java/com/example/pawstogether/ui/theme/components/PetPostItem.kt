package com.example.pawstogether.ui.theme.components

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import coil.compose.rememberAsyncImagePainter
import com.example.pawstogether.model.Comment
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.ui.theme.models.PostAction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(UnstableApi::class)
@Composable
fun PetPostItem(
    post: PetPost,
    currentUserId: String,
    onPostInteraction: (PostAction) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var showComments by remember { mutableStateOf(false) }
    var actualUserName by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        getCurrentUserName(currentUserId) { username ->
            actualUserName = username
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (post.isVideo) {
                VideoPlayer(post.mediaUrl)
            } else {
                Image(
                    painter = rememberAsyncImagePainter(post.mediaUrl),
                    contentDescription = "Imagen de la publicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (post.likedBy.contains(currentUserId)) {
                            onPostInteraction(PostAction.Unlike(post.id))
                        } else {
                            onPostInteraction(PostAction.Like(post.id))
                        }
                    }
                ) {
                    Text(if (post.likedBy.contains(currentUserId)) "Unlike" else "Like")
                }

                Text("${post.likes} likes", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showComments = !showComments }) {
                Text(if (showComments) "Ocultar comentarios" else "Ver comentarios")
            }

            if (showComments) {
                // Lista de comentarios existentes
                post.comments.forEach { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${comment.userName}: ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Campo para agregar nuevo comentario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Añadir un comentario...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            if (commentText.isNotEmpty() && actualUserName.isNotEmpty()) {
                                Log.d("PetPostItem", "Enviando comentario: $commentText")  // Agregar log
                                // Llamar a onPostInteraction con el texto del comentario
                                onPostInteraction(PostAction.Comment(post.id, commentText))
                                commentText = ""
                            } else {
                                Log.d("PetPostItem", "Comentario o nombre de usuario vacío")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar comentario")
                    }
                }
            }
        }
    }
}

// Función auxiliar para obtener el nombre del usuario
@OptIn(UnstableApi::class)
suspend fun getCurrentUserName(userId: String, onComplete: (String) -> Unit) {
    try {
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(userId).get().await()
        val userName = userDoc.getString("userName") ?: ""
        onComplete(userName)
    } catch (e: Exception) {
        Log.e("CommentSection", "Error al obtener el nombre del usuario", e)
        onComplete("")
    }
}