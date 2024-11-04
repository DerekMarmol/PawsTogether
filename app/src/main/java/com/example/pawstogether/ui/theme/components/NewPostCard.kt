package com.example.pawstogether.ui.theme.components

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


@Composable
fun NewPostCard(
    onNewPost: (String, String) -> Unit,
    currentUserId: String,
    currentUserName: String
) {
    var newPostUri by remember { mutableStateOf<Uri?>(null) }
    var newPostDescription by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var actualUserName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUserId) {
        Log.d("NewPostCard", "currentUserId: $currentUserId")
        Utils.getCurrentUserName(currentUserId) { username ->
            actualUserName = username
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Crear Publicación", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            MediaPicker(
                onMediaSelected = { uri, fileName ->
                    newPostUri = uri
                    selectedFileName = fileName
                }
            )

            selectedFileName?.let {
                Text("Archivo seleccionado: $it", style = MaterialTheme.typography.bodySmall)
            }

            newPostUri?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                MediaPreview(uri)
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newPostDescription,
                onValueChange = { newPostDescription = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        newPostUri?.let { uri ->
                            try {
                                val storage = FirebaseStorage.getInstance()
                                val fileName = "media/${UUID.randomUUID()}"
                                val storageRef = storage.reference.child(fileName)
                                val uploadTask = storageRef.putFile(uri).await()
                                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true

                                // Solo llamamos a onNewPost con la URL y descripción
                                // Ya no guardamos el post directamente aquí
                                onNewPost(downloadUrl, newPostDescription)

                                // Limpiar los campos después de publicar
                                newPostUri = null
                                newPostDescription = ""
                                selectedFileName = null
                            } catch (e: Exception) {
                                Log.e("NewPostCard", "Error al subir el archivo", e)
                                Toast.makeText(context, "Error al crear la publicación: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && newPostUri != null
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