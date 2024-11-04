package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
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
import com.example.pawstogether.viewmodel.ChatViewModel
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
    val chatViewModel: ChatViewModel = viewModel()
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
                            currentUserId = auth.currentUser?.uid ?: "",
                            chatViewModel = chatViewModel
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

        OutlinedTextField(
            value = petName,
            onValueChange = { petName = it },
            label = { Text("Nombre de la Mascota*") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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

                        val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
                        imageRef.putFile(imageUri!!)
                            .addOnSuccessListener { imageTaskSnapshot ->
                                imageTaskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                                    val uploadMedicalHistory = medicalHistoryUri?.let { uri ->
                                        val medicalRef = storage.reference.child("medicalHistory/${UUID.randomUUID()}")
                                        medicalRef.putFile(uri).continueWith { task ->
                                            if (task.isSuccessful) {
                                                task.result?.storage?.downloadUrl
                                            } else null
                                        }
                                    }

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
    currentUserId: String,
    chatViewModel: ChatViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditForm by remember { mutableStateOf(false) }
    var showAdoptionDialog by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Dialog para marcar como adoptado
    if (showAdoptionDialog) {
        AlertDialog(
            onDismissRequest = { showAdoptionDialog = false },
            title = { Text("Confirmar adopción") },
            text = { Text("¿Estás seguro de que esta mascota ha sido adoptada?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("adoptionPets")
                            .document(pet.id)
                            .update("status", "adopted")
                            .addOnSuccessListener {
                                Log.d("AdoptionPetItem", "Mascota marcada como adoptada")
                                showAdoptionDialog = false
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdoptionPetItem", "Error al marcar como adoptada", e)
                            }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdoptionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta publicación? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (pet.imageUrl.isNotEmpty()) {
                            val imageRef = storage.getReferenceFromUrl(pet.imageUrl)
                            imageRef.delete()
                        }

                        pet.medicalHistoryUrl?.let { url ->
                            if (url.isNotEmpty()) {
                                val medicalRef = storage.getReferenceFromUrl(url)
                                medicalRef.delete()
                            }
                        }

                        db.collection("adoptionPets")
                            .document(pet.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("AdoptionPetItem", "Publicación eliminada con éxito")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdoptionPetItem", "Error al eliminar la publicación", e)
                            }

                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Formulario de edición
    if (showEditForm) {
        EditAdoptionForm(
            pet = pet,
            onDismiss = { showEditForm = false },
            onUpdate = { updatedPet ->
                db.collection("adoptionPets")
                    .document(pet.id)
                    .set(updatedPet)
                    .addOnSuccessListener {
                        Log.d("AdoptionPetItem", "Publicación actualizada con éxito")
                        showEditForm = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("AdoptionPetItem", "Error al actualizar la publicación", e)
                    }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Publicado por: ${pet.userName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Badge de estado adoptado
                if (pet.status == "adopted") {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "Adoptado",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Adoptado",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }

            // Contenido principal de la tarjeta
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

            if (pet.medicalHistoryUrl?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* Implementar vista del historial médico */ },
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

            // Botones de acción
            if (currentUserId != pet.userId) {
                if (pet.status != "adopted") {
                    Button(
                        onClick = {
                            chatViewModel.startAdoptionChat(
                                otherUserId = pet.userId,
                                otherUserName = pet.userName,
                                petName = pet.petName
                            )
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Adoptado",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "¡Esta mascota ya encontró un hogar!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { showEditForm = true },
                            modifier = Modifier.weight(1f),
                            enabled = pet.status != "adopted"
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
                            onClick = { showDeleteDialog = true },
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

                    if (pet.status != "adopted") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAdoptionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Marcar como adoptado",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marcar como adoptado")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAdoptionForm(
    pet: AdoptionPet,
    onDismiss: () -> Unit,
    onUpdate: (AdoptionPet) -> Unit
) {
    var petName by remember { mutableStateOf(pet.petName) }
    var species by remember { mutableStateOf(pet.species) }
    var breed by remember { mutableStateOf(pet.breed) }
    var age by remember { mutableStateOf(pet.age.toString()) }
    var description by remember { mutableStateOf(pet.description) }
    var isNeutered by remember { mutableStateOf(pet.isNeutered) }
    var hasVaccines by remember { mutableStateOf(pet.hasVaccines) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var medicalHistoryUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val storage = FirebaseStorage.getInstance()

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    val medicalHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> medicalHistoryUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Mascota en Adopción") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it },
                    label = { Text("Nombre de la Mascota*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Especie*") },
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

                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cambiar Imagen")
                }

                imageUri?.let {
                    Text(
                        text = "Nueva imagen seleccionada: ${it.lastPathSegment}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = { medicalHistoryLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cambiar Historial Médico")
                }

                medicalHistoryUri?.let {
                    Text(
                        text = "Nuevo archivo seleccionado: ${it.lastPathSegment}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (petName.isNotBlank() && species.isNotBlank() &&
                        age.isNotBlank() && description.isNotBlank()) {
                        isLoading = true

                        fun updatePetData(newImageUrl: String? = null, newMedicalHistoryUrl: String? = null) {
                            val updatedPet = pet.copy(
                                petName = petName,
                                species = species,
                                breed = breed,
                                age = age.toIntOrNull() ?: 0,
                                description = description,
                                isNeutered = isNeutered,
                                hasVaccines = hasVaccines,
                                imageUrl = newImageUrl ?: pet.imageUrl,
                                medicalHistoryUrl = newMedicalHistoryUrl ?: pet.medicalHistoryUrl
                            )
                            onUpdate(updatedPet)
                        }

                        if (imageUri != null) {
                            val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
                            imageRef.putFile(imageUri!!)
                                .addOnSuccessListener { imageTaskSnapshot ->
                                    imageTaskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                                        if (medicalHistoryUri != null) {
                                            val medicalRef = storage.reference.child("medicalHistory/${UUID.randomUUID()}")
                                            medicalRef.putFile(medicalHistoryUri!!)
                                                .addOnSuccessListener { medicalTaskSnapshot ->
                                                    medicalTaskSnapshot.storage.downloadUrl.addOnSuccessListener { medicalUrl ->
                                                        updatePetData(imageUrl.toString(), medicalUrl.toString())
                                                    }
                                                }
                                        } else {
                                            updatePetData(newImageUrl = imageUrl.toString())
                                        }
                                    }
                                }
                        } else if (medicalHistoryUri != null) {
                            val medicalRef = storage.reference.child("medicalHistory/${UUID.randomUUID()}")
                            medicalRef.putFile(medicalHistoryUri!!)
                                .addOnSuccessListener { medicalTaskSnapshot ->
                                    medicalTaskSnapshot.storage.downloadUrl.addOnSuccessListener { medicalUrl ->
                                        updatePetData(newMedicalHistoryUrl = medicalUrl.toString())
                                    }
                                }
                        } else {
                            updatePetData()
                        }
                    }
                },
                enabled = petName.isNotBlank() && species.isNotBlank() &&
                        age.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Actualizar")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun NavController.navigateToChat(userId: String, userName: String) {
    this.navigate("chat/$userId/$userName")
}

