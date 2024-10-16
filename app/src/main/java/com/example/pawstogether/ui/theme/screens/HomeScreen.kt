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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.model.Comment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.pawstogether.ui.theme.screens.ReportsScreen
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    // Elimina esta línea:
    // val navController = rememberNavController()

    var currentUserId by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentUserName by remember { mutableStateOf("") }

    var petPosts by remember { mutableStateOf(listOf<PetPost>()) }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        auth.currentUser?.let { user ->
            currentUserId = user.uid
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    currentUserName = document.getString("userName") ?: ""
                }
        }

        try {
            db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("HomeScreen", "Error al escuchar cambios en posts", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        petPosts = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PetPost::class.java)?.copy(id = doc.id)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error al configurar el listener de posts", e)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = {
                        navController.navigate("profile")
                        scope.launch { drawerState.close() }
                    }
                )
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Report, contentDescription = "Reports") },
                    label = { Text("Reportes") },
                    selected = false,
                    onClick = {
                        navController.navigate("reports")
                        scope.launch { drawerState.close() }
                    }
                )
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Pets, contentDescription = "Adoption") },
                    label = { Text("Adopción") },
                    selected = false,
                    onClick = {
                        navController.navigate("adoption")
                        scope.launch { drawerState.close() }
                    }
                )
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Configuración") },
                    selected = false,
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    }
                )
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.RateReview, contentDescription = "Reviews") },
                    label = { Text("Reseñas") },
                    selected = false,
                    onClick = {
                        val userId = "someUserId"  // Asegúrate de tener un valor correcto aquí
                        val serviceType = "someServiceType"  // Lo mismo para el tipo de servicio
                        navController.navigate("rating/$userId/$serviceType")
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PawsTogether") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
            HomeContent(
                paddingValues = paddingValues,
                petPosts = petPosts,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                onNewPost = { newPost ->
                    scope.launch {
                        try {
                            val postWithTimestamp = newPost.copy(
                                timestamp = System.currentTimeMillis(),
                                userId = currentUserId,
                                userName = currentUserName
                            )
                            val docRef = db.collection("posts").add(postWithTimestamp).await()
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error al guardar el nuevo post", e)
                        }
                    }
                },
                onPostInteraction = { action ->
                    scope.launch {
                        try {
                            when (action) {
                                is PostAction.Like -> {
                                    db.collection("posts").document(action.postId)
                                        .update(
                                            "likes", FieldValue.increment(1),
                                            "likedBy", FieldValue.arrayUnion(currentUserId)
                                        )
                                }
                                is PostAction.Unlike -> {
                                    db.collection("posts").document(action.postId)
                                        .update(
                                            "likes", FieldValue.increment(-1),
                                            "likedBy", FieldValue.arrayRemove(currentUserId)
                                        )
                                }
                                is PostAction.Comment -> {
                                    val newComment = Comment(userId = currentUserId, userName = currentUserName, text = action.text)
                                    db.collection("posts").document(action.postId)
                                        .update("comments", FieldValue.arrayUnion(newComment))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error al actualizar el post", e)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    petPosts: List<PetPost>,
    currentUserId: String,
    currentUserName: String,
    onNewPost: (PetPost) -> Unit,
    onPostInteraction: (PostAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            NewPostCard(
                onNewPost = onNewPost,
                currentUserId = currentUserId,
                currentUserName = currentUserName
            )
        }

        items(petPosts) { post ->
            PetPostItem(
                post = post,
                currentUserId = currentUserId,
                onPostInteraction = onPostInteraction
            )
        }
    }
}

@Composable
fun NewPostCard(
    onNewPost: (PetPost) -> Unit,
    currentUserId: String,
    currentUserName: String
) {
    var newPostUri by remember { mutableStateOf<Uri?>(null) }
    var newPostDescription by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Crear Publicación", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            MediaPicker(
                onMediaSelected = { uri, fileName ->
                    newPostUri = uri
                    selectedFileName = fileName
                }
            )

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
                onValueChange = { newPostDescription = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        newPostUri?.let { uri ->
                            try {
                                val storage = FirebaseStorage.getInstance()
                                val fileName = "media/${UUID.randomUUID()}"
                                val storageRef = storage.reference.child(fileName)
                                val uploadTask = storageRef.putFile(uri).await()
                                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                                val newPost = PetPost(
                                    id = UUID.randomUUID().toString(),
                                    userId = "currentUserId", // Reemplaza esto con el ID real del usuario
                                    mediaUrl = downloadUrl,
                                    description = newPostDescription,
                                    isVideo = isVideo,
                                    likes = 0,
                                    likedBy = emptyList(),
                                    comments = emptyList(),
                                    timestamp = System.currentTimeMillis()
                                )
                                saveNewPost(newPost)
                                // No necesitas llamar a onNewPost aquí, ya que el listener se encargará de actualizar la lista
                                // Limpiar los campos después de publicar
                                newPostUri = null
                                newPostDescription = ""
                                selectedFileName = null
                            } catch (e: Exception) {
                                Log.e("NewPostCard", "Error al subir el archivo", e)
                                // Mostrar un mensaje de error al usuario
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Publicar")
                }
            }
        }
    }
}


suspend fun saveNewPost(post: PetPost) {
    try {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts").add(post).await()
    } catch (e: Exception) {
        Log.e("Firestore", "Error al guardar el post", e)
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