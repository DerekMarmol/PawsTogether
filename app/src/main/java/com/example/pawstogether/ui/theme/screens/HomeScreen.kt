package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.model.Comment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var petPosts by remember { mutableStateOf(listOf<PetPost>()) }
    var newPostUri by remember { mutableStateOf<Uri?>(null) }
    var newPostDescription by remember { mutableStateOf("") }

    // Simular carga inicial de datos
    LaunchedEffect(Unit) {
        petPosts = listOf(
            PetPost(id = "1", userId = "user1", mediaUrl = "https://example.com/image1.jpg", description = "Mi perro jugando", isVideo = false),
            PetPost(id = "2", userId = "user2", mediaUrl = "https://example.com/video1.mp4", description = "Mi gato durmiendo", isVideo = true)
        )
        Log.d("HomeScreen", "Datos iniciales cargados: ${petPosts.size} posts")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("PawsTogether") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Contenido principal
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Formulario para nueva publicación
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Crear Publicación", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Media Picker
                        MediaPicker { uri ->
                            newPostUri = uri
                            Log.d("HomeScreen", "Nuevo medio seleccionado: $uri")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPostDescription,
                            onValueChange = { newPostDescription = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newPostUri != null && newPostDescription.isNotEmpty()) {
                                    val newPost = PetPost(
                                        id = UUID.randomUUID().toString(),
                                        userId = "currentUserId",
                                        mediaUrl = newPostUri.toString(),
                                        description = newPostDescription,
                                        isVideo = newPostUri.toString().endsWith(".mp4", ignoreCase = true)
                                    )
                                    petPosts = listOf(newPost) + petPosts
                                    newPostUri = null
                                    newPostDescription = ""
                                    Log.d("HomeScreen", "Nueva publicación añadida: ${newPost.id}")
                                } else {
                                    Log.e("HomeScreen", "Error al publicar: URI o descripción vacía")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Publicar")
                        }
                    }
                }
            }

            // Feed de publicaciones
            items(petPosts) { post ->
                PetPostItem(
                    post = post,
                    onLikePost = { postId ->
                        petPosts = petPosts.map {
                            if (it.id == postId) it.copy(likes = it.likes + 1) else it
                        }
                        Log.d("HomeScreen", "Like añadido a post: $postId")
                    },
                    onCommentPost = { postId, commentText ->
                        petPosts = petPosts.map {
                            if (it.id == postId) {
                                val newComment = Comment(userId = "currentUserId", text = commentText)
                                it.copy(comments = it.comments + newComment)
                            } else it
                        }
                        Log.d("HomeScreen", "Comentario añadido a post: $postId")
                    }
                )
            }
        }
    }
}

@Composable
fun PetPostItem(post: PetPost, onLikePost: (String) -> Unit, onCommentPost: (String, String) -> Unit) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Media (Imagen o Video)
            if (post.isVideo) {
                VideoPlayer(url = post.mediaUrl)
            } else {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Descripción
            Text(post.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { onLikePost(post.id) }) {
                    Text("Me gusta (${post.likes})")
                }
                Button(onClick = {
                    if (commentText.isNotEmpty()) {
                        onCommentPost(post.id, commentText)
                        commentText = ""
                    }
                }) {
                    Text("Comentar")
                }
            }

            // Campo de comentario
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Añadir un comentario") },
                modifier = Modifier.fillMaxWidth()
            )

            // Lista de comentarios
            if (post.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Comentarios:", style = MaterialTheme.typography.titleSmall)
                post.comments.forEach { comment ->
                    Text("${comment.userId}: ${comment.text}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MediaPicker(onMediaSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                onMediaSelected(uri)
            }
        }
    )

    Button(onClick = {
        // Permite seleccionar tanto imágenes como videos
        launcher.launch("*/*")  // MIME para aceptar cualquier tipo de archivo
    }) {
        Text("Seleccionar Medio (Imagen o Video)")
    }
}



@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { context ->
            StyledPlayerView(context).apply {
                player = exoPlayer
            }
        }
    )
}