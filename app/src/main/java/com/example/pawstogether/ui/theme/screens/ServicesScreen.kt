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
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pawstogether.model.Service
import com.example.pawstogether.model.ServiceRequest
import com.example.pawstogether.viewmodel.ChatViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = viewModel()
) {
    val currentUser = Firebase.auth.currentUser
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var serviceType by remember { mutableStateOf("") }
    var serviceDescription by remember { mutableStateOf("") }
    var serviceCost by remember { mutableStateOf("") }
    var isFreeService by remember { mutableStateOf(false) }
    var servicesList by remember { mutableStateOf<List<Service>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var serviceRequests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    var serviceTypeError by remember { mutableStateOf<String?>(null) }
    var serviceDescriptionError by remember { mutableStateOf<String?>(null) }
    var serviceCostError by remember { mutableStateOf<String?>(null) }
    var editingService by remember { mutableStateOf<Service?>(null) }

    fun resetFields() {
        serviceType = ""
        serviceDescription = ""
        serviceCost = ""
        isFreeService = false
        serviceTypeError = null
        serviceDescriptionError = null
        serviceCostError = null
        editingService = null
    }

    fun loadServiceToEdit(service: Service) {
        serviceType = service.serviceType
        serviceDescription = service.serviceDescription
        serviceCost = if (service.isFreeService) "" else service.serviceCost
        isFreeService = service.isFreeService
        editingService = service
        showDialog = true
    }

    fun loadServiceRequests(serviceId: String) {
        db.collection("serviceRequests")
            .whereEqualTo("serviceId", serviceId)
            .get()
            .addOnSuccessListener { result ->
                serviceRequests = result.map { doc ->
                    doc.toObject(ServiceRequest::class.java)
                }
            }
    }

    fun loadServices() {
        db.collection("services")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                servicesList = result.map { document ->
                    Service(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        userName = document.getString("userName") ?: "",
                        serviceType = document.getString("serviceType") ?: "",
                        serviceDescription = document.getString("serviceDescription") ?: "",
                        serviceCost = document.getString("serviceCost") ?: "",
                        isFreeService = document.getBoolean("isFreeService") ?: false,
                        timestamp = document.getTimestamp("timestamp")
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ServicesScreen", "Error al obtener los servicios", exception)
            }
    }

    fun updateService() {
        editingService?.let { service ->
            val serviceData = hashMapOf(
                "serviceType" to serviceType,
                "serviceDescription" to serviceDescription,
                "serviceCost" to if (isFreeService) "Gratis" else serviceCost,
                "isFreeService" to isFreeService,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("services")
                .document(service.id)
                .update(serviceData.toMap())
                .addOnSuccessListener {
                    Log.d("ServicesScreen", "Servicio actualizado exitosamente")
                    loadServices()
                    showDialog = false
                    resetFields()
                }
                .addOnFailureListener { e ->
                    Log.e("ServicesScreen", "Error al actualizar el servicio", e)
                }
        }
    }

    fun updateRequestConfirmation(requestId: String, isProvider: Boolean) {
        val updateField = if (isProvider) "isProviderConfirmed" else "isRequesterConfirmed"

        db.collection("serviceRequests")
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->
                val request = document.toObject(ServiceRequest::class.java)
                request?.let {
                    val updates = hashMapOf<String, Any>(
                        updateField to true
                    )

                    if ((isProvider && request.isRequesterConfirmed) ||
                        (!isProvider && request.isProviderConfirmed)) {
                        updates["status"] = "confirmed"
                    }

                    db.collection("serviceRequests")
                        .document(requestId)
                        .update(updates)
                        .addOnSuccessListener {
                            loadServiceRequests(request.serviceId)
                        }
                }
            }
    }

    fun publishService() {
        val serviceData = hashMapOf(
            "userId" to (currentUser?.uid ?: ""),
            "userName" to (currentUser?.displayName ?: ""),
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

    fun requestService(service: Service) {
        val request = ServiceRequest(
            serviceId = service.id,
            providerId = service.userId,
            requesterId = currentUser?.uid ?: "",
            providerName = service.userName,
            requesterName = currentUser?.displayName ?: "",
            serviceType = service.serviceType
        )

        db.collection("serviceRequests")
            .add(request)
            .addOnSuccessListener {
                chatViewModel.startServiceChat(
                    otherUserId = service.userId,
                    otherUserName = service.userName,
                    serviceType = service.serviceType
                )
                navController.navigate("chat/${service.userId}/${service.userName}")
            }
    }

    fun deleteService(serviceId: String) {
        db.collection("services")
            .document(serviceId)
            .delete()
            .addOnSuccessListener {
                loadServices()
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn {
                items(servicesList) { service ->
                    ServiceCard(
                        service = service,
                        isOwner = service.userId == currentUser?.uid,
                        onEdit = { loadServiceToEdit(service) },
                        onDelete = { deleteService(service.id) },
                        onViewRequests = {
                            selectedService = service
                            loadServiceRequests(service.id)
                            showRequestsDialog = true
                        },
                        onRequestService = { requestService(service) }
                    )
                }
            }
        }

        if (showRequestsDialog && selectedService != null) {
            AlertDialog(
                onDismissRequest = { showRequestsDialog = false },
                title = { Text("Solicitudes de Servicio") },
                text = {
                    LazyColumn {
                        items(serviceRequests) { request ->
                            ServiceRequestItem(
                                request = request,
                                isProvider = currentUser?.uid == request.providerId,
                                onConfirm = {
                                    updateRequestConfirmation(
                                        request.id,
                                        currentUser?.uid == request.providerId
                                    )
                                },
                                onChatClick = {
                                    val chatUserId = if (currentUser?.uid == request.providerId)
                                        request.requesterId else request.providerId
                                    val chatUserName = if (currentUser?.uid == request.providerId)
                                        request.requesterName else request.providerName
                                    navController.navigate("chat/$chatUserId/$chatUserName")
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRequestsDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    resetFields()
                },
                title = { Text(if (editingService == null) "Agregar Servicio" else "Editar Servicio") },
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
                                if (editingService != null) {
                                    updateService()
                                } else {
                                    publishService()
                                }
                            }
                        }
                    }) {
                        Text(if (editingService == null) "Publicar Servicio" else "Actualizar Servicio")
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
}

@Composable
fun ServiceCard(
    service: Service,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewRequests: () -> Unit,
    onRequestService: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.serviceType,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Ofrecido por: ${service.userName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Descripción: ${service.serviceDescription}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Costo: ${if (service.isFreeService) "Gratis" else service.serviceCost}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Eliminar")
                    }
                    IconButton(onClick = onViewRequests) {
                        Icon(Icons.Default.People, "Ver solicitudes")
                    }
                }
            } else {
                Button(
                    onClick = onRequestService,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Solicitar servicio")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Solicitar Servicio")
                }
            }
        }
    }
}

@Composable
fun ServiceRequestItem(
    request: ServiceRequest,
    isProvider: Boolean,
    onConfirm: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = if (isProvider) request.requesterName else request.providerName)
                    Text(
                        text = "Estado: ${request.status}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (request.status == "pending") {
                        IconButton(onClick = onConfirm) {
                            Icon(
                                imageVector = if ((isProvider && !request.isProviderConfirmed) ||
                                    (!isProvider && !request.isRequesterConfirmed))
                                    Icons.Default.CheckBoxOutlineBlank
                                else Icons.Default.CheckBox,
                                contentDescription = "Confirmar"
                            )
                        }
                    }
                    IconButton(onClick = onChatClick) {
                        Icon(Icons.Default.Chat, "Chat")
                    }
                }
            }
        }
    }
}


