package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.pawstogether.model.PetPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun ReportsScreen() {
    var reportDescription by remember { mutableStateOf("") }
    var reportLocation by remember { mutableStateOf("") }
    var reportDate by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("Perdida") }
    var reportUri by remember { mutableStateOf<Uri?>(null) }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var petReports by remember { mutableStateOf(listOf<PetPost>()) }
    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        auth.currentUser?.let { user ->
            currentUserId = user.uid
        }

        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ReportsScreen", "Error al realizar cambios en reportes", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    petReports = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PetPost::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reportar Mascota", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(15.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tipo de reporte: ")
            Spacer(modifier = Modifier.width(4.dp))
            RadioButton(
                selected = reportType == "Perdida",
                onClick = { reportType = "Perdida" },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Text("Perdida")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = reportType == "Encontrada",
                onClick = { reportType = "Encontrada" },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Text("Encontrada")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reportDescription,
            onValueChange = { reportDescription = it },
            label = { Text("Descripción de la mascota") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reportLocation,
            onValueChange = { reportLocation = it },
            label = { Text("Lugar") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reportDate,
            onValueChange = { reportDate = it },
            label = { Text("Fecha (dd/mm/yyyy)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        MediaPicker { uri, _ ->
            reportUri = uri
        }

        reportUri?.let { uri ->
            MediaPreview(uri)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    reportUri?.let { uri ->
                        try {
                            val storage = FirebaseStorage.getInstance()
                            val fileName = "reports/${UUID.randomUUID()}"
                            val storageRef = storage.reference.child(fileName)
                            val uploadTask = storageRef.putFile(uri).await()
                            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                            val report = PetPost(
                                id = UUID.randomUUID().toString(),
                                userId = "currentUserId",
                                mediaUrl = downloadUrl,
                                description = reportDescription,
                                isVideo = false,
                                likes = 0,
                                likedBy = emptyList(),
                                comments = emptyList(),
                                timestamp = System.currentTimeMillis(),
                                location = reportLocation,
                                date = reportDate,
                                reportType = reportType
                            )

                            db.collection("reports").add(report).await()
                            reportDescription = ""
                            reportLocation = ""
                            reportDate = ""
                            reportUri = null
                        } catch (e: Exception) {
                            Log.e("ReportsScreen", "Error al crear el reporte", e)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reportar")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp)
        ) {
            items(petReports) { report ->
                PetPostItem(
                    post = report,
                    currentUserId = currentUserId,
                    onPostInteraction = {  }
                )
            }
        }
    }
}

