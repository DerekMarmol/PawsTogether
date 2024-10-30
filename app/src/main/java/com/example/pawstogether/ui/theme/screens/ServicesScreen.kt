package com.example.pawstogether.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    var showDialog by remember { mutableStateOf(false) }
    var serviceTypeError by remember { mutableStateOf<String?>(null) }
    var serviceDescriptionError by remember { mutableStateOf<String?>(null) }
    var serviceCostError by remember { mutableStateOf<String?>(null) }

    fun resetFields() {
        serviceType = ""
        serviceDescription = ""
        serviceCost = ""
        isFreeService = false
        serviceTypeError = null
        serviceDescriptionError = null
        serviceCostError = null
    }

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
                showDialog = false
                resetFields()
            }
            .addOnFailureListener { e ->
                Log.e("ServicesScreen", "Error al publicar el servicio", e)
            }
    }

    fun validateFields(): Boolean {
        var isValid = true

        if (serviceType.isBlank()) {
            serviceTypeError = "El tipo de servicio es obligatorio"
            isValid = false
        } else {
            serviceTypeError = null
        }

        if (serviceDescription.isBlank()) {
            serviceDescriptionError = "La descripción es obligatoria"
            isValid = false
        } else {
            serviceDescriptionError = null
        }

        if (!isFreeService && serviceCost.isBlank()) {
            serviceCostError = "El costo es obligatorio si no es un servicio gratuito"
            isValid = false
        } else {
            serviceCostError = null
        }

        return isValid
    }

    LaunchedEffect(Unit) {
        loadServices()
    }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Servicio")
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn {
                    items(servicesList) { service ->
                        ServiceCard(service)
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Agregar Servicio") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = serviceType,
                                onValueChange = { serviceType = it },
                                label = { Text("Tipo de servicio (ej. Paseo, Cuidado, Baño)") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = serviceTypeError != null,
                                supportingText = { serviceTypeError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = serviceDescription,
                                onValueChange = { serviceDescription = it },
                                label = { Text("Descripción del servicio") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = serviceDescriptionError != null,
                                supportingText = { serviceDescriptionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = serviceCost,
                                    onValueChange = {
                                        if (!isFreeService) {
                                            serviceCost = it.filter { char -> char.isDigit() || char == '.' }
                                            if (serviceCost.contains(".") && serviceCost.split(".")[1].length > 2) {
                                                serviceCost = serviceCost.substring(0, serviceCost.indexOf(".") + 3)
                                            }
                                        }
                                    },
                                    label = { Text("Costo del servicio") },
                                    enabled = !isFreeService,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = serviceCostError != null,
                                    supportingText = { serviceCostError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isFreeService,
                                    onCheckedChange = {
                                        isFreeService = it
                                        if (it) serviceCost = ""
                                    }
                                )
                                Text("Ofrecer este servicio de forma gratuita")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (validateFields()) {
                                coroutineScope.launch {
                                    publishService()
                                }
                            }
                        }) {
                            Text("Publicar Servicio")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                            resetFields()
                        }) {
                            Text("Cancelar")
                        }
                    }
                )
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
            Text("${service.serviceType}", style = MaterialTheme.typography.titleMedium)
            Text("Descripción: ${service.serviceDescription}", style = MaterialTheme.typography.bodyMedium)
            Text("Costo: ${if (service.isFreeService) "Gratis" else service.serviceCost}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { /* Despues se añadira la logica para contratar el servicio */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Contratar Servicio")
            }
        }
    }
}
