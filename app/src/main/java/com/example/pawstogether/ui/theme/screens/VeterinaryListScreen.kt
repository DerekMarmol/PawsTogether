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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinaryListScreen(navController: NavHostController) {
    val clinics = remember { mutableStateListOf<Clinic>() }
    var showAddClinicDialog by remember { mutableStateOf(false) }
    var showReviews by remember { mutableStateOf(false) }
    var showReviewForm by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("clinics")
            .get()
            .addOnSuccessListener { result ->
                clinics.clear()
                for (document in result) {
                    val clinic = document.toObject(Clinic::class.java)
                    clinics.add(clinic)
                }
            }
            .addOnFailureListener { e ->
                Log.w("VeterinaryListScreen", "Error al cargar las clínicas", e)
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clínicas Veterinarias Cercanas") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddClinicDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Clínica")
            }
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyColumn {
                    items(clinics) { clinic ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = clinic.name, style = MaterialTheme.typography.headlineMedium)
                                Text(text = "Tel: ${clinic.phone}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Dirección: ${clinic.address}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Horario: ${clinic.hours}", style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(onClick = {
                                    showReviews = !showReviews
                                }) {
                                    Text(if (showReviews) "Ocultar Reseñas" else "Mostrar Reseñas")
                                }

                                if (showReviews) {
                                    ClinicReviewList(clinicId = clinic.id)
                                }

                                TextButton(onClick = {
                                    showReviewForm = !showReviewForm
                                }) {
                                    Text(if (showReviewForm) "Ocultar Formulario de Reseña" else "Agregar Reseña")
                                }

                                if (showReviewForm) {
                                    ClinicReviewForm(clinicId = clinic.id)
                                }
                            }
                        }
                    }
                }

                if (showAddClinicDialog) {
                    AddClinicDialog(onDismiss = { showAddClinicDialog = false }) { newClinic ->
                        clinics.add(newClinic)
                        showAddClinicDialog = false
                    }
                }
            }
        }
    )
}


@Composable
fun AddClinicDialog(onDismiss: () -> Unit, onAddClinic: (Clinic) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Clínica Veterinaria") },
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
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Horarios de Atención") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty() && hours.isNotEmpty()) {
                    val newClinic = Clinic(UUID.randomUUID().toString(), name, phone, address, hours)

                    val db = FirebaseFirestore.getInstance()
                    db.collection("clinics")
                        .document(newClinic.id)
                        .set(newClinic)
                        .addOnSuccessListener {
                            Log.d("AddClinicDialog", "Clínica añadida correctamente")
                            onAddClinic(newClinic)
                        }
                        .addOnFailureListener { e ->
                            Log.w("AddClinicDialog", "Error al añadir la clínica", e)
                        }
                }
            }) {
                Text("Agregar")
            }

        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class Clinic(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val hours: String = ""
)

@Composable
fun ClinicReviewForm(clinicId: String) {
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
            text = "Califica esta clínica",
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
            Checkbox(
                checked = isThankYou,
                onCheckedChange = { isThankYou = it }
            )
            Text("Marcar como agradecimiento especial")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val reviewData = hashMapOf(
                    "userId" to (currentUser?.uid ?: ""),
                    "clinicId" to clinicId,
                    "stars" to stars,
                    "review" to review,
                    "isThankYou" to isThankYou,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("clinics/$clinicId/reviews")
                    .add(reviewData)
                    .addOnSuccessListener {
                        Log.d("ClinicReviewForm", "Reseña subida correctamente")
                        stars = 0
                        review = ""
                        isThankYou = false
                    }
                    .addOnFailureListener { e ->
                        Log.w("ClinicReviewForm", "Error al subir la reseña", e)
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
fun ClinicReviewList(clinicId: String) {
    var reviews by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(clinicId) {
        db.collection("clinics/$clinicId/reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ClinicReviewList", "Error al cargar las reseñas", e)
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
