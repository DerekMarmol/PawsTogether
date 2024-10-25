package com.example.pawstogether.ui.theme.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pawstogether.model.PetReport
import com.example.pawstogether.ui.theme.components.MediaPicker
import com.example.pawstogether.ui.theme.components.MediaPreview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    navigateToSearch: () -> Unit
) {
    var reportType by remember { mutableStateOf("Perdida") }
    var petType by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var petColor by remember { mutableStateOf("") }
    var petName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var reportUri by remember { mutableStateOf<Uri?>(null) }
    var reports by remember { mutableStateOf(listOf<PetReport>()) }
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showFilters by remember { mutableStateOf(false) }
    var selectedReportTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedPetTypeFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val petTypes = remember(reports) {
        reports.map { it.petType }.distinct().filterNot { it.isBlank() }
    }

    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val filteredReports = remember(reports, selectedReportTypeFilter, selectedPetTypeFilter, searchQuery) {
        reports.filter { report ->
            val matchesReportType = selectedReportTypeFilter == null || report.reportType == selectedReportTypeFilter
            val matchesPetType = selectedPetTypeFilter == null || report.petType == selectedPetTypeFilter
            val matchesSearch = searchQuery.isEmpty() ||
                    report.location.contains(searchQuery, ignoreCase = true) ||
                    report.description.contains(searchQuery, ignoreCase = true) ||
                    report.petBreed.contains(searchQuery, ignoreCase = true)

            matchesReportType && matchesPetType && matchesSearch
        }
    }

    LaunchedEffect(Unit) {
        try {
            db.collection("pet_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ReportsScreen", "Error loading reports", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        reports = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PetReport::class.java)?.copy(id = doc.id)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("ReportsScreen", "Error setting up snapshot listener", e)
        }
    }

    fun clearForm() {
        reportType = "Perdida"
        petType = ""
        petBreed = ""
        petColor = ""
        petName = ""
        description = ""
        location = ""
        contactPhone = ""
        contactEmail = ""
        reportUri = null
        showForm = false
    }

    suspend fun publishReport() {
        try {
            isLoading = true
            val currentUser = auth.currentUser
            if (currentUser != null) {
                var mediaUrl = ""

                reportUri?.let { uri ->
                    val storage = FirebaseStorage.getInstance()
                    val fileName = "pet_reports/${UUID.randomUUID()}"
                    val storageRef = storage.reference.child(fileName)
                    val uploadTask = storageRef.putFile(uri).await()
                    mediaUrl = uploadTask.storage.downloadUrl.await().toString()
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                val report = PetReport(
                    userId = currentUser.uid,
                    petType = petType,
                    petBreed = petBreed,
                    petColor = petColor,
                    petName = petName,
                    description = description,
                    location = location,
                    date = dateFormat.format(Date()),
                    reportType = reportType,
                    mediaUrl = mediaUrl,
                    contactPhone = contactPhone,
                    contactEmail = contactEmail,
                    timestamp = System.currentTimeMillis()
                )

                db.collection("pet_reports").add(report).await()
                clearForm()
            }
        } catch (e: Exception) {
            Log.e("ReportsScreen", "Error creating report", e)
        } finally {
            isLoading = false
        }
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Filtrar Reportes") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tipo de Reporte",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedReportTypeFilter == "Perdida",
                            onClick = {
                                selectedReportTypeFilter = if (selectedReportTypeFilter == "Perdida") null else "Perdida"
                            },
                            label = { Text("Perdidas") }
                        )

                        FilterChip(
                            selected = selectedReportTypeFilter == "Encontrada",
                            onClick = {
                                selectedReportTypeFilter = if (selectedReportTypeFilter == "Encontrada") null else "Encontrada"
                            },
                            label = { Text("Encontradas") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tipo de Mascota",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        petTypes.forEach { type ->
                            FilterChip(
                                selected = selectedPetTypeFilter == type,
                                onClick = {
                                    selectedPetTypeFilter = if (selectedPetTypeFilter == type) null else type
                                },
                                label = { Text(type) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilters = false }) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedReportTypeFilter = null
                        selectedPetTypeFilter = null
                        searchQuery = ""
                        showFilters = false
                    }
                ) {
                    Text("Limpiar Filtros")
                }
            }
        )
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("Nuevo Reporte de Mascota") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = reportType == "Perdida",
                                    onClick = { reportType = "Perdida" }
                                )
                                Text("Mascota Perdida")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = reportType == "Encontrada",
                                    onClick = { reportType = "Encontrada" }
                                )
                                Text("Mascota Encontrada")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = petType,
                                onValueChange = { petType = it },
                                label = { Text("Tipo de Mascota (perro, gato, etc.)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = petBreed,
                                onValueChange = { petBreed = it },
                                label = { Text("Raza") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = petColor,
                                onValueChange = { petColor = it },
                                label = { Text("Color/Características distintivas") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = petName,
                                onValueChange = { petName = it },
                                label = { Text("Nombre de la mascota (si se conoce)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Descripción detallada") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Lugar donde se perdió/encontró") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Teléfono de contacto") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Email de contacto") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            MediaPicker { uri, _ ->
                                reportUri = uri
                            }

                            reportUri?.let { uri ->
                                MediaPreview(uri)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            publishReport()
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Publicar Reporte")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { clearForm() },
                    enabled = !isLoading
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Reportes de Mascotas") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                        }
                    }
                )

                // Barra de búsqueda
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Buscar por ubicación, raza...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                }

                // Chips de filtros activos
                if (selectedReportTypeFilter != null || selectedPetTypeFilter != null) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedReportTypeFilter?.let {
                            AssistChip(
                                onClick = { selectedReportTypeFilter = null },
                                label = { Text(it) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar filtro",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        selectedPetTypeFilter?.let {
                            AssistChip(
                                onClick = { selectedPetTypeFilter = null },
                                label = { Text(it) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar filtro",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showForm = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Reporte")
            }
        }
    ) { paddingValues ->
        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron reportes con los filtros seleccionados",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredReports) { report ->
                    ReportCard(report = report)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: PetReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mascota ${report.reportType}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = report.date,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (report.mediaUrl.isNotEmpty()) {
                AsyncImage(
                    model = report.mediaUrl,
                    contentDescription = "Foto de la mascota",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Tipo: ${report.petType}")
            Text("Raza: ${report.petBreed}")
            if (report.petName.isNotEmpty()) {
                Text("Nombre: ${report.petName}")
            }
            Text("Color/Características: ${report.petColor}")
            Text("Ubicación: ${report.location}")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Información de contacto:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Teléfono: ${report.contactPhone}")
            Text("Email: ${report.contactEmail}")
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            } else {
                currentRow.add(placeable)
                currentRowWidth += placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row -> row.maxOf { it.height } }

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width
                }
                y += row.maxOf { it.height }
            }
        }
    }
}