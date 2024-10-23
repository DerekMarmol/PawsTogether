package com.example.pawstogether.ui.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.ui.theme.models.PostAction

@Composable
fun PetPostItem(
    post: PetPost,
    currentUserId: String,
    onPostInteraction: (PostAction) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var showComments by remember { mutableStateOf(false) }

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
                post.comments.forEach { comment ->
                    Text(
                        text = "${comment.userName}: ${comment.text}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Escribe un comentario") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (commentText.isNotEmpty()) {
                            onPostInteraction(PostAction.Comment(post.id, commentText))
                            commentText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Comentar")
                }
            }
        }
    }
}