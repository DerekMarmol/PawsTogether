package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileScreen(
    onProfileUpdated: () -> Unit = {}
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Launcher para seleccionar imagen
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            // Subir imagen a Firebase Storage
            isLoading = true
            val storageRef = storage.reference.child("profile_images/${currentUser?.uid}")
            storageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            // Guardar URL en Firestore
                            currentUser?.uid?.let { uid ->
                                firestore.collection("users").document(uid)
                                    .update("profileImageUrl", downloadUri.toString())
                                    .addOnSuccessListener {
                                        isLoading = false
                                    }
                                    .addOnFailureListener { exception ->
                                        error = "Error al guardar la imagen: ${exception.message}"
                                        isLoading = false
                                    }
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    error = "Error al subir la imagen: ${exception.message}"
                    isLoading = false
                }
        }
    }

    // Efecto para cargar datos del usuario
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        displayName = document.getString("displayName") ?: ""
                        username = document.getString("username") ?: ""
                        val imageUrl = document.getString("profileImageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            profileImageUri = Uri.parse(imageUrl)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    error = "Error al cargar datos: ${exception.message}"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagen de perfil
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { launcher.launch("image/*") }
        ) {
            if (profileImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(profileImageUri)
                            .build()
                    ),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Icono por defecto cuando no hay imagen
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil por defecto",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Icono de edición
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar foto",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Campos editables
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nombre") },
            enabled = isEditing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RectangleShape, true)
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de usuario") },
            enabled = isEditing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RectangleShape, true)
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (isEditing) {
                        // Guardar cambios en Firestore
                        currentUser?.uid?.let { uid ->
                            isLoading = true
                            val userUpdates = hashMapOf(
                                "displayName" to displayName,
                                "username" to username
                            )
                            firestore.collection("users").document(uid)
                                .update(userUpdates as Map<String, Any>)
                                .addOnSuccessListener {
                                    isEditing = false
                                    isLoading = false
                                    onProfileUpdated()
                                }
                                .addOnFailureListener { exception ->
                                    error = "Error al guardar los cambios: ${exception.message}"
                                    isLoading = false
                                }
                        }
                    } else {
                        isEditing = true
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (isEditing) "Guardar" else "Editar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEditing) {
                Button(
                    onClick = {
                        isEditing = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        "Cancelar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}