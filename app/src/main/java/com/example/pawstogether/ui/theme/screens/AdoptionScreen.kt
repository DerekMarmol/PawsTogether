package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.pawstogether.model.AdoptionPet
import com.example.pawstogether.model.PetPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@Composable
fun AdoptionScreen() {
    val db = FirebaseFirestore.getInstance()
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Mascota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            if (showForm) {
                AddAdoptionForm(onSubmit = { newPet ->
                    db.collection("adoptionPets")
                        .add(newPet)
                        .addOnSuccessListener {
                            Log.d("AdoptionScreen", "Mascota agregada con éxito")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdoptionScreen", "Error al agregar mascota", e)
                        }
                    showForm = false
                })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(adoptionPets) { pet ->
                        AdoptionPetItem(pet)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAdoptionForm(onSubmit: (AdoptionPet) -> Unit) {
    var petName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var medicalHistoryUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val medicalHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        medicalHistoryUri = uri
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Nombre de la Mascota") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    medicalHistoryLauncher.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar Historial Médico")
            }

            medicalHistoryUri?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Archivo seleccionado: ${it.lastPathSegment}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { imageLauncher.launch("image/*") }) {
                Text("Seleccionar Imagen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (petName.isNotBlank() && description.isNotBlank() && imageUri != null && medicalHistoryUri != null) {
                        isLoading = true

                        val storage = FirebaseStorage.getInstance()

                        val imageFileName = "images/${UUID.randomUUID()}"
                        val imageRef = storage.reference.child(imageFileName)

                        imageRef.putFile(imageUri!!)
                            .addOnSuccessListener { imageTaskSnapshot ->
                                imageTaskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->

                                    val medicalFileName = "medicalHistory/${UUID.randomUUID()}"
                                    val medicalRef = storage.reference.child(medicalFileName)

                                    medicalRef.putFile(medicalHistoryUri!!)
                                        .addOnSuccessListener { medicalTaskSnapshot ->
                                            medicalTaskSnapshot.storage.downloadUrl.addOnSuccessListener { medicalHistoryUrl ->

                                                val newPet = AdoptionPet(
                                                    petName = petName,
                                                    description = description,
                                                    medicalHistoryUrl = medicalHistoryUrl.toString(),
                                                    imageUrl = imageUrl.toString()
                                                )

                                                onSubmit(newPet)
                                                isLoading = false
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e("AddAdoptionForm", "Error al subir el archivo de historial médico", exception)
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
                enabled = petName.isNotBlank() && description.isNotBlank() && imageUri != null && medicalHistoryUri != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Publicar en Adopción")
                }
            }
        }
    }
}


@Composable
fun AdoptionPetItem(pet: AdoptionPet) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp, bottom = 8.dp)
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (pet.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = pet.imageUrl,
                    contentDescription = "Imagen de ${pet.petName}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = pet.petName, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = pet.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {  }) {
                Text("Enviar solicitud de adopción")
            }
        }
    }
}

