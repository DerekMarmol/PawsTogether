package com.example.pawstogether.ui.theme.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCareScreen(navController: NavHostController) {
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var showCreateArticleDialog by remember { mutableStateOf(false) }
    var showReviews by remember { mutableStateOf(false) }
    var showReviewForm by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("recommendations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("PetCareScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }

                posts = snapshot?.documents?.map { document ->
                    Post(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        content = document.getString("content") ?: "",
                        videoUrl = document.getString("videoUrl"),
                        imageUrl = document.getString("imageUrl"),
                        guideUrl = document.getString("guideUrl")
                    )
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicar Recomendación") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        navController.navigate("veterinary_list")
                    }) {
                        Text("Clínicas Veterinarias")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateArticleDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Artículo")
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Publicaciones Recientes",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                LazyColumn {
                    items(posts) { post ->
                        PostItem(post = post, showReviews, showReviewForm, onToggleReviewVisibility = {
                            showReviews = !showReviews
                        }, onToggleReviewFormVisibility = {
                            showReviewForm = !showReviewForm
                        })
                    }
                }

                if (showCreateArticleDialog) {
                    CreateArticleDialog(onDismiss = { showCreateArticleDialog = false })
                }
            }
        }
    )
}




@Composable
fun CreateArticleDialog(onDismiss: () -> Unit) {
    var articleTitle by remember { mutableStateOf("") }
    var articleContent by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var guideUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val storage = FirebaseStorage.getInstance()
    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri = uri }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
    val guidePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> guideUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Artículo") },
        text = {
            Column {
                OutlinedTextField(
                    value = articleTitle,
                    onValueChange = { articleTitle = it },
                    label = { Text("Título del Artículo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = articleContent,
                    onValueChange = { articleContent = it },
                    label = { Text("Contenido del Artículo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Subir Video")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Subir Imagen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { guidePickerLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Subir Documento")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    uploadRecommendationToFirebase(
                        title = articleTitle,
                        content = articleContent,
                        videoUri = videoUri,
                        imageUri = imageUri,
                        guideUri = guideUri,
                        context = context,
                        storage = storage
                    )
                    onDismiss()
                }
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PostItem(
    post: Post,
    showReviews: Boolean,
    showReviewForm: Boolean,
    onToggleReviewVisibility: () -> Unit,
    onToggleReviewFormVisibility: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = post.title, style = MaterialTheme.typography.headlineLarge)
        Text(text = post.content, style = MaterialTheme.typography.bodyLarge)

        post.videoUrl?.let { videoUrl ->
            Text(text = "", style = MaterialTheme.typography.bodyMedium)
            AndroidView(factory = {
                VideoView(it).apply {
                    setVideoURI(Uri.parse(videoUrl))
                    setMediaController(MediaController(it).apply {
                        setAnchorView(this@apply)
                    })
                    start()
                }
            }, modifier = Modifier
                .fillMaxWidth()
                .height(200.dp))
        }

        post.imageUrl?.let { imageUrl ->
            Text(text = "Imagen:", style = MaterialTheme.typography.bodyMedium)
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            )
        }

        post.guideUrl?.let { guideUrl ->
            Text(text = "Guía/Documento:", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(guideUrl))
                context.startActivity(intent)
            }) {
                Text("Descargar Documento")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onToggleReviewFormVisibility) {
            Text(if (showReviewForm) "Ocultar" else "Crea una Reseña")
        }

        if (showReviewForm) {
            ReviewForm(postId = post.id)
        }

        TextButton(onClick = onToggleReviewVisibility) {
            Text(if (showReviews) "Ocultar Reseñas" else "Mostrar Reseñas")
        }

        if (showReviews) {
            ReviewList(postId = post.id)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
    }
}


data class Post(
    val id: String,
    val title: String,
    val content: String,
    val videoUrl: String?,
    val imageUrl: String?,
    val guideUrl: String?
)

fun uploadRecommendationToFirebase(
    title: String,
    content: String,
    videoUri: Uri?,
    imageUri: Uri?,
    guideUri: Uri?,
    context: Context,
    storage: FirebaseStorage
) {
    val db = FirebaseFirestore.getInstance()
    val newPost = hashMapOf(
        "title" to title,
        "content" to content,
        "timestamp" to FieldValue.serverTimestamp()
    )

    val uploadTasks = mutableListOf<Task<Uri>>()

    videoUri?.let { uri ->
        val videoRef = storage.reference.child("recommendations/videos/${UUID.randomUUID()}")
        val uploadTask = videoRef.putFile(uri)
        uploadTasks.add(uploadTask.continueWithTask {
            videoRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            newPost["videoUrl"] = downloadUrl.toString()
        })
    }

    imageUri?.let { uri ->
        val imageRef = storage.reference.child("recommendations/images/${UUID.randomUUID()}")
        val uploadTask = imageRef.putFile(uri)
        uploadTasks.add(uploadTask.continueWithTask {
            imageRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            newPost["imageUrl"] = downloadUrl.toString()
        })
    }

    guideUri?.let { uri ->
        val guideRef = storage.reference.child("recommendations/guides/${UUID.randomUUID()}")
        val uploadTask = guideRef.putFile(uri)
        uploadTasks.add(uploadTask.continueWithTask {
            guideRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            newPost["guideUrl"] = downloadUrl.toString()
        })
    }

    Tasks.whenAllComplete(uploadTasks).addOnSuccessListener {
        db.collection("recommendations")
            .add(newPost)
            .addOnSuccessListener {
                Toast.makeText(context, "Publicación subida correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al subir la publicación", Toast.LENGTH_SHORT).show()
            }
    }
}


@Composable
fun ReviewForm(postId: String) {
    var stars by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    var isThankYou by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Califica este artículo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

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
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val reviewData = hashMapOf(
                    "userId" to (currentUser?.uid ?: ""),
                    "postId" to postId,
                    "stars" to stars,
                    "review" to review,
                    "isThankYou" to isThankYou,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                // Guardar la reseña en Firestore
                db.collection("posts/$postId/reviews")
                    .add(reviewData)
                    .addOnSuccessListener {
                        Log.d("ReviewForm", "Reseña subida correctamente")
                        stars = 0
                        review = ""
                        isThankYou = false
                    }
                    .addOnFailureListener { e ->
                        Log.w("ReviewForm", "Error al subir la reseña", e)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = stars > 0 && review.isNotEmpty()
        ) {
            Text("Enviar Reseña")
        }
    }
}


@Composable
fun ReviewList(postId: String) {
    var reviews by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(postId) {
        db.collection("posts/$postId/reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ReviewList", "Error al cargar las reseñas", e)
                    return@addSnapshotListener
                }

                reviews = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Reseñas", style = MaterialTheme.typography.headlineMedium)

        reviews.forEach { review ->
            ReviewItem(
                stars = (review["stars"] as? Long ?: 0).toInt(),
                content = review["review"] as? String ?: "",
                isThankYou = review["isThankYou"] as? Boolean ?: false
            )
        }
    }
}

@Composable
fun ReviewItem(stars: Int, content: String, isThankYou: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < stars) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = content, style = MaterialTheme.typography.bodyLarge)

        if (isThankYou) {
            Text(
                text = "Agradecimiento especial",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
    }
}


