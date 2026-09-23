package com.example.ctma.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Componente interactivo para previsualizar imágenes de evidencia.
 * Permite ver la foto en miniatura y ampliarla a pantalla completa al pulsarla.
 */
@Composable
fun ImagenEvidencia(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember(uriString) { mutableStateOf(true) }
    var errorCarga by remember(uriString) { mutableStateOf(false) }
    var mostrarModalAmpliado by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        cargando = true
        errorCarga = false
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val loadedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } catch (e: Exception) {
                        decodeBitmapConFallback(context, uri)
                    }
                } else {
                    decodeBitmapConFallback(context, uri)
                }

                if (loadedBitmap != null) {
                    bitmap = loadedBitmap.asImageBitmap()
                    errorCarga = false
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
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = bitmap != null) { mostrarModalAmpliado = true },
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "Evidencia Fotográfica",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🔍", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Toca para ampliar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            errorCarga -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("📷", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Imagen de evidencia no disponible",
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

    // Modal para ver la imagen en pantalla completa
    if (mostrarModalAmpliado && bitmap != null) {
        Dialog(onDismissRequest = { mostrarModalAmpliado = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Evidencia Fotográfica",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "Evidencia Fotográfica Ampliada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { mostrarModalAmpliado = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

private fun decodeBitmapConFallback(context: android.content.Context, uri: Uri): android.graphics.Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: if (uri.path != null && File(uri.path!!).exists()) {
                File(uri.path!!).inputStream()
            } else null

        inputStream?.use { stream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Reducir escala para optimizar memoria
            }
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (e: Exception) {
        null
    }
}
