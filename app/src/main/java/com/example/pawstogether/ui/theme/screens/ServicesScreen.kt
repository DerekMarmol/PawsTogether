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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

data class Service(
    val serviceType: String = "",
    val serviceDescription: String = "",
    val serviceCost: String = "",
    val isFreeService: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var serviceType by remember { mutableStateOf("") }
    var serviceDescription by remember { mutableStateOf("") }
    var serviceCost by remember { mutableStateOf("") }
    var isFreeService by remember { mutableStateOf(false) }

    var servicesList by remember { mutableStateOf<List<Service>>(emptyList()) }

    fun loadServices() {
        db.collection("services")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val services = result.map { document ->
                    Service(
                        serviceType = document.getString("serviceType") ?: "",
                        serviceDescription = document.getString("serviceDescription") ?: "",
                        serviceCost = document.getString("serviceCost") ?: "",
                        isFreeService = document.getBoolean("isFreeService") ?: false
                    )
                }
                servicesList = services
            }
            .addOnFailureListener { exception ->
                Log.e("ServicesScreen", "Error al obtener los servicios", exception)
            }
    }

    fun publishService() {
        val serviceData = hashMapOf(
            "serviceType" to serviceType,
            "serviceDescription" to serviceDescription,
            "serviceCost" to if (isFreeService) "Gratis" else serviceCost,
            "isFreeService" to isFreeService,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("services")
            .add(serviceData)
            .addOnSuccessListener {
                Log.d("ServicesScreen", "Servicio publicado exitosamente")
                loadServices()
            }
            .addOnFailureListener { e ->
                Log.e("ServicesScreen", "Error al publicar el servicio", e)
            }
    }

    LaunchedEffect(Unit) {
        loadServices()
    }

    val currencySymbol = "Q. "

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ofrecer Servicios") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Elige el tipo de servicio", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    label = { Text("Tipo de servicio (ej. Paseo, Cuidado, Baño)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serviceDescription,
                    onValueChange = { serviceDescription = it },
                    label = { Text("Descripción del servicio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currencySymbol, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = serviceCost,
                        onValueChange = {
                            if (!isFreeService) {
                                serviceCost = it.filterIndexed { index, char ->
                                    char.isDigit() || (char == '.' && it.indexOf('.') == index)
                                }

                                if (serviceCost.contains(".")) {
                                    val parts = serviceCost.split(".")
                                    if (parts.size > 1 && parts[1].length > 2) {
                                        serviceCost = parts[0] + "." + parts[1].take(2)
                                    }
                                }
                            }
                        },
                        label = { Text("Costo del servicio (deja vacío si es gratis)") },
                        enabled = !isFreeService,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFreeService,
                        onCheckedChange = { isFreeService = it }
                    )
                    Text("Ofrecer este servicio de forma gratuita")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            publishService()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publicar Servicio")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(servicesList) { service ->
                        ServiceCard(service)
                    }
                }
            }
        }
    )
}

@Composable
fun ServiceCard(service: Service) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tipo de Servicio: ${service.serviceType}", style = MaterialTheme.typography.titleMedium)
            Text("Descripción: ${service.serviceDescription}", style = MaterialTheme.typography.bodyMedium)
            Text("Costo: ${if (service.isFreeService) "Gratis" else service.serviceCost}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}