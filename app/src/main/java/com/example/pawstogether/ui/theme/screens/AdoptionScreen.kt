package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pawstogether.model.AdoptionPet
import com.example.pawstogether.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionScreen(
    navController: NavController,
) {
    val viewModel: ProfileViewModel = viewModel()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var adoptionPets by remember { mutableStateOf(listOf<AdoptionPet>()) }
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("adoptionPets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdoptionScreen", "Error al escuchar cambios en adopción de mascotas", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    adoptionPets = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AdoptionPet::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adopciones Disponibles") },
                actions = {
                    IconButton(onClick = { showForm = true }) {
                        Icon(Icons.Default.Add, "Agregar Mascota")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (showForm) {
                AddAdoptionForm(
                    onSubmit = { newPet ->
                        // Agregamos la información del usuario actual
                        val petWithUserInfo = newPet.copy(
                            userId = auth.currentUser?.uid ?: "",
                            userName = viewModel.displayName
                        )

                        db.collection("adoptionPets")
                            .add(petWithUserInfo)
                            .addOnSuccessListener {
                                Log.d("AdoptionScreen", "Mascota agregada con éxito")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdoptionScreen", "Error al agregar mascota", e)
                            }
                        showForm = false
                    },
                    onCancel = { showForm = false }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(adoptionPets) { pet ->
                        AdoptionPetItem(
                            pet = pet,
                            navController = navController,
                            currentUserId = auth.currentUser?.uid ?: ""
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdoptionForm(
    onSubmit: (AdoptionPet) -> Unit,
    onCancel: () -> Unit
) {
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isNeutered by remember { mutableStateOf(false) }
    var hasVaccines by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var medicalHistoryUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val medicalHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> medicalHistoryUri = uri }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Nueva Mascota en Adopción",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Campos básicos
        OutlinedTextField(
            value = petName,
            onValueChange = { petName = it },
            label = { Text("Nombre de la Mascota*") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de especie editable
        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text("Especie*") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ej: Perro, Gato, Conejo...") }
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
            onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
            label = { Text("Edad (años)*") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción*") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Checkboxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isNeutered,
                onCheckedChange = { isNeutered = it }
            )
            Text("Castrado/Esterilizado")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hasVaccines,
                onCheckedChange = { hasVaccines = it }
            )
            Text("Tiene vacunas")
        }

        // Botones de archivo
        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar Imagen*")
        }

        imageUri?.let {
            Text(
                text = "Imagen seleccionada: ${it.lastPathSegment}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = { medicalHistoryLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar Historial Médico (Opcional)")
        }

        medicalHistoryUri?.let {
            Text(
                text = "Archivo seleccionado: ${it.lastPathSegment}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    if (petName.isNotBlank() && species.isNotBlank() &&
                        age.isNotBlank() && description.isNotBlank() && imageUri != null) {
                        isLoading = true
                        val storage = FirebaseStorage.getInstance()

                        // Subir imagen
                        val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
                        imageRef.putFile(imageUri!!)
                            .addOnSuccessListener { imageTaskSnapshot ->
                                imageTaskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                                    // Si hay historial médico, subirlo
                                    val uploadMedicalHistory = medicalHistoryUri?.let { uri ->
                                        val medicalRef = storage.reference.child("medicalHistory/${UUID.randomUUID()}")
                                        medicalRef.putFile(uri).continueWith { task ->
                                            if (task.isSuccessful) {
                                                task.result?.storage?.downloadUrl
                                            } else null
                                        }
                                    }

                                    // Procesar la subida del historial médico si existe
                                    if (uploadMedicalHistory != null) {
                                        uploadMedicalHistory.addOnSuccessListener { medicalHistoryUrl ->
                                            val newPet = AdoptionPet(
                                                petName = petName,
                                                species = species,
                                                breed = breed,
                                                age = age.toIntOrNull() ?: 0,
                                                description = description,
                                                isNeutered = isNeutered,
                                                hasVaccines = hasVaccines,
                                                medicalHistoryUrl = medicalHistoryUrl?.toString() ?: "",
                                                imageUrl = imageUrl.toString()
                                            )
                                            onSubmit(newPet)
                                            isLoading = false
                                        }
                                    } else {
                                        // Si no hay historial médico, crear el objeto sin él
                                        val newPet = AdoptionPet(
                                            petName = petName,
                                            species = species,
                                            breed = breed,
                                            age = age.toIntOrNull() ?: 0,
                                            description = description,
                                            isNeutered = isNeutered,
                                            hasVaccines = hasVaccines,
                                            imageUrl = imageUrl.toString()
                                        )
                                        onSubmit(newPet)
                                        isLoading = false
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AddAdoptionForm", "Error al subir la imagen", exception)
                                isLoading = false
                            }
                    }
                },
                enabled = petName.isNotBlank() && species.isNotBlank() &&
                        age.isNotBlank() && description.isNotBlank() && imageUri != null,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
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

@Composable
fun AdoptionPetItem(
    pet: AdoptionPet,
    navController: NavController,
    currentUserId: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con información del usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Publicado por: ${pet.userName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (pet.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = pet.imageUrl,
                    contentDescription = "Imagen de ${pet.petName}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pet.petName,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "${pet.species} • ${pet.age} años",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pet.breed.isNotBlank()) {
                Text(
                    text = "Raza: ${pet.breed}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Estado de salud
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                if (pet.isNeutered) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Castrado") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Castrado"
                            )
                        }
                    )
                }
                if (pet.hasVaccines) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = { },
                        label = { Text("Vacunado") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Vacunado"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pet.description,
                style = MaterialTheme.typography.bodyLarge
            )

            // Mostrar enlace al historial médico si existe
            if (pet.medicalHistoryUrl?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        // Aquí puedes implementar la lógica para descargar o ver el historial médico
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Ver historial médico",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver historial médico")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Solo mostrar el botón de chat si no es el propietario de la publicación
            if (currentUserId != pet.userId) {
                Button(
                    onClick = {
                        navController.navigateToChat(
                            userId = pet.userId,
                            userName = pet.userName
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Iniciar chat",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar para adopción")
                }
            } else {
                // Si es el propietario, mostrar opciones de edición/eliminación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            // Implementar lógica de edición
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar publicación",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Implementar lógica de eliminación
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar publicación",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

// Extensión para la navegación al chat
fun NavController.navigateToChat(userId: String, userName: String) {
    this.navigate("chat/$userId/$userName")
}

