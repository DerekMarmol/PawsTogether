package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.model.Comment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var petPosts by remember { mutableStateOf(listOf<PetPost>()) }
    var newPostUri by remember { mutableStateOf<Uri?>(null) }
    var newPostDescription by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PawsTogether") },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                NewPostCard(
                    newPostUri = newPostUri,
                    newPostDescription = newPostDescription,
                    selectedFileName = selectedFileName,
                    onMediaSelected = { uri, fileName ->
                        newPostUri = uri
                        selectedFileName = fileName
                    },
                    onDescriptionChanged = { newPostDescription = it },
                    onPostSubmit = { uri, description, context -> // <- Aceptar contexto
                        if (uri != null && description.isNotEmpty()) {
                            val newUri = uri.toString()
                            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                            val newPost = PetPost(
                                id = UUID.randomUUID().toString(),
                                userId = "currentUserId",
                                mediaUrl = newUri,
                                description = description,
                                isVideo = isVideo,
                                likes = 0,
                                likedBy = emptyList()
                            )
                            petPosts = listOf(newPost) + petPosts
                            newPostUri = null
                            newPostDescription = ""
                            selectedFileName = null
                            Log.d("HomeScreen", "Nueva publicación añadida: ${newPost.id}")
                        } else {
                            Log.e("HomeScreen", "Error al publicar: URI o descripción vacía")
                        }
                    }
                )
            }

            items(petPosts) { post ->
                PetPostItem(
                    post = post,
                    onPostInteraction = { action ->
                        when (action) {
                            is PostAction.Like -> {
                                petPosts = petPosts.map {
                                    if (it.id == action.postId) {
                                        it.copy(
                                            likes = it.likes + 1,
                                            likedBy = it.likedBy + "currentUserId"
                                        )
                                    } else it
                                }
                            }
                            is PostAction.Unlike -> {
                                petPosts = petPosts.map {
                                    if (it.id == action.postId) {
                                        it.copy(
                                            likes = it.likes - 1,
                                            likedBy = it.likedBy - "currentUserId"
                                        )
                                    } else it
                                }
                            }
                            is PostAction.Comment -> {
                                petPosts = petPosts.map {
                                    if (it.id == action.postId) {
                                        val newComment = Comment(userId = "currentUserId", text = action.text)
                                        it.copy(comments = it.comments + newComment)
                                    } else it
                                }
                            }
                        }
                    }
                )
            }
        }

        if (showMenu) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Perfil") },
                    onClick = { /* Navegar a perfil */ }
                )
                DropdownMenuItem(
                    text = { Text("Configuración") },
                    onClick = { /* Navegar a configuración */ }
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión") },
                    onClick = { /* Lógica para cerrar sesión */ }
                )
            }
        }
    }
}


@Composable
fun NewPostCard(
    newPostUri: Uri?,
    newPostDescription: String,
    selectedFileName: String?,
    onMediaSelected: (Uri, String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPostSubmit: (Uri?, String, android.content.Context) -> Unit // <- Pasar contexto
) {
    val context = LocalContext.current // <- Obtener el contexto aquí

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Crear Publicación", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            MediaPicker(onMediaSelected)

            selectedFileName?.let {
                Text("Archivo seleccionado: $it", style = MaterialTheme.typography.bodySmall)
            }

            newPostUri?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                MediaPreview(uri)
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newPostDescription,
                onValueChange = onDescriptionChanged,
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onPostSubmit(newPostUri, newPostDescription, context) }, // <- Pasar contexto aquí
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publicar")
            }
        }
    }
}

@Composable
fun MediaPicker(onMediaSelected: (Uri, String) -> Unit) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    onMediaSelected(uri, fileName)
                }
            }
        }
    )

    Button(onClick = { launcher.launch("*/*") }) {
        Text("Seleccionar Medio (Imagen o Video)")
    }
}

@Composable
fun MediaPreview(uri: Uri) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val type = contentResolver.getType(uri)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        when {
            type?.startsWith("image/") == true -> {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Previsualización de imagen",
                    modifier = Modifier.fillMaxSize()
                )
            }
            type?.startsWith("video/") == true -> {
                VideoPlayer(uri.toString())
            }
            else -> {
                Text("Tipo de archivo no soportado", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun PetPostItem(
    post: PetPost,
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
                        if (post.likedBy.contains("currentUserId")) {
                            onPostInteraction(PostAction.Unlike(post.id))
                        } else {
                            onPostInteraction(PostAction.Like(post.id))
                        }
                    }
                ) {
                    Text(if (post.likedBy.contains("currentUserId")) "Unlike" else "Like")
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
                        text = "${comment.userId}: ${comment.text}",
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

@Composable
fun VideoPlayer(uri: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                controllerAutoShow = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        update = { playerView ->
            playerView.player = exoPlayer
        }
    )
}

sealed class PostAction {
    data class Like(val postId: String) : PostAction()
    data class Unlike(val postId: String) : PostAction()
    data class Comment(val postId: String, val text: String) : PostAction()
}