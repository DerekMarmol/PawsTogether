package com.example.pawstogether.ui.theme.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun MediaPicker(onMediaSelected: (Uri, String) -> Unit) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    onMediaSelected(uri, fileName)
                }
            }
        }
    )

    Button(onClick = { launcher.launch("*/*") }) {
        Text("Seleccionar Medio (Imagen o Video)")
    }
}

@Composable
fun MediaPreview(uri: Uri) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val type = contentResolver.getType(uri)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        when {
            type?.startsWith("image/") == true -> {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Previsualización de imagen",
                    modifier = Modifier.fillMaxSize()
                )
            }
            type?.startsWith("video/") == true -> {
                VideoPlayer(uri.toString())
            }
            else -> {
                Text("Tipo de archivo no soportado", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun VideoPlayer(uri: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                controllerAutoShow = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        update = { playerView ->
            playerView.player = exoPlayer
        }
    )
}