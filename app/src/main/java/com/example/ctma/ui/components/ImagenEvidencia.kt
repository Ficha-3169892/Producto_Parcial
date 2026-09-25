package com.example.ctma.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImagenEvidencia(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember(uriString) { mutableStateOf(true) }
    var errorCarga by remember(uriString) { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        cargando = true
        errorCarga = false
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    null
                } ?: run {
                    val path = uri.path
                    if (path != null && File(path).exists()) {
                        File(path).inputStream()
                    } else null
                }

                if (inputStream != null) {
                    inputStream.use { stream ->
                        val loadedBitmap = BitmapFactory.decodeStream(stream)
                        if (loadedBitmap != null) {
                            bitmap = loadedBitmap.asImageBitmap()
                            errorCarga = false
                        } else {
                            errorCarga = true
                        }
                    }
                } else {
                    errorCarga = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorCarga = true
            } finally {
                cargando = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Evidencia Fotográfica",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            errorCarga -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🖼️", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No se pudo visualizar la imagen de evidencia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            cargando -> {
                CircularProgressIndicator()
            }
        }
    }
}
