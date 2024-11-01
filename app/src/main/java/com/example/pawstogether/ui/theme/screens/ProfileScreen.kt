package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.pawstogether.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val age: String = "",
    val health: String = "",
    val vaccinationHistory: String = ""
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onProfileUpdated: () -> Unit = {}
) {
    var displayName by remember { mutableStateOf(viewModel.displayName) }
    var username by remember { mutableStateOf(viewModel.username) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var pets by remember { mutableStateOf(listOf<Pet>()) }
    var showAddPetDialog by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            isLoading = true
            val storageRef = storage.reference.child("profile_images/${currentUser?.uid}")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            currentUser?.uid?.let { uid ->
                                firestore.collection("users").document(uid)
                                    .update("profileImageUrl", downloadUri.toString())
                                    .addOnSuccessListener { isLoading = false }
                                    .addOnFailureListener { exception ->
                                        error = "Error al guardar la imagen: ${exception.message}"
                                        isLoading = false
                                    }
                            }
                        }
                }
        }
    }

    // Remove the updateDisplayName method call
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

            firestore.collection("users").document(uid)
                .collection("pets")
                .get()
                .addOnSuccessListener { documents ->
                    pets = documents.map { doc ->
                        Pet(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = doc.getString("type") ?: "",
                            breed = doc.getString("breed") ?: "",
                            age = doc.getString("age") ?: "",
                            health = doc.getString("health") ?: "",
                            vaccinationHistory = doc.getString("vaccinationHistory") ?: ""
                        )
                    }
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Image with editing option
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil por defecto",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        viewModel.updateProfile(
                            displayName,
                            username,
                            onSuccess = {
                                isLoading = false
                                isEditing = false
                                onProfileUpdated()
                            },
                            onError = { errorMessage ->
                                isLoading = false
                                error = errorMessage
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Guardar cambios")
                    }
                }
            } else {
                Text(
                    text = "Nombre: $displayName",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Usuario: $username",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar perfil")
                }
            }
        }

        item {
            Button(
                onClick = { showAddPetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Añadir Mascota")
            }
        }

        items(pets) { pet ->
            PetCard(
                pet = pet,
                isEditing = isEditing,
                onDelete = { petId ->
                    currentUser?.uid?.let { uid ->
                        firestore.collection("users").document(uid)
                            .collection("pets").document(petId)
                            .delete()
                            .addOnSuccessListener {
                                pets = pets.filter { it.id != petId }
                            }
                    }
                }
            )
        }

        if (showAddPetDialog) {
            item {
                AddPetDialog(
                    onDismiss = { showAddPetDialog = false },
                    onPetAdded = { newPet ->
                        currentUser?.uid?.let { uid ->
                            firestore.collection("users").document(uid)
                                .collection("pets")
                                .add(newPet)
                                .addOnSuccessListener { documentReference ->
                                    pets = pets + newPet.copy(id = documentReference.id)
                                    showAddPetDialog = false
                                }
                        }
                    }
                )
            }
        }

        if (error.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PetCard(
    pet: Pet,
    isEditing: Boolean,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pet.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isEditing) {
                    IconButton(onClick = { onDelete(pet.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar mascota"
                        )
                    }
                }
            }
            Text("Tipo: ${pet.type}")
            Text("Raza: ${pet.breed}")
            Text("Edad: ${pet.age}")
            Text("Salud: ${pet.health}")
            Text("Historial de vacunación: ${pet.vaccinationHistory}")
        }
    }
}

@Composable
fun AddPetDialog(
    onDismiss: () -> Unit,
    onPetAdded: (Pet) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var health by remember { mutableStateOf("") }
    var vaccinationHistory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar nueva mascota") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Tipo de animal") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raza") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Edad") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = health,
                    onValueChange = { health = it },
                    label = { Text("Salud") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = vaccinationHistory,
                    onValueChange = { vaccinationHistory = it },
                    label = { Text("Historial de vacunación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPet = Pet(
                        name = name,
                        type = type,
                        breed = breed,
                        age = age,
                        health = health,
                        vaccinationHistory = vaccinationHistory
                    )
                    onPetAdded(newPet)
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}