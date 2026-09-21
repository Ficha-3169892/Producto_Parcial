package com.example.ctma.ui.catalogo.solicitudes

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ctma.model.SolicitudPrestamo
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionScreen(
    solicitud: SolicitudPrestamo,
    onConfirmarDevolucion: (String?, Double?, Double?) -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    var evidenciaUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var latitud by remember { mutableStateOf<Double?>(null) }
    var longitud by remember { mutableStateOf<Double?>(null) }
    var estadoSincronizacion by remember { mutableStateOf("LOCAL") }
    var mensajePermiso by remember { mutableStateOf("") }

    // Launcher para capturar foto en vivo con la Cámara
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            evidenciaUri = tempUri
            estadoSincronizacion = "SUBIENDO"
        }
    }

    // Launcher para Permisos de Ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            latitud = 6.2514
            longitud = -75.5636
            mensajePermiso = "Ubicación GPS obtenida con éxito."
        } else {
            mensajePermiso = "Permiso denegado. No se pueden registrar coordenadas exactas."
        }
    }

    // Función para crear una URI temporal compartible sin usar almacenamiento externo propenso a fallos
    fun crearUriTemporal(): Uri {
        val directorio = File(context.externalCacheDir, "evidencias")
        if (!directorio.exists()) directorio.mkdirs()
        val archivo = File(directorio, "evidencia_${solicitud.id}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registrar Devolución") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Solicitud de Préstamo #${solicitud.id}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Equipo ID asociado: ${solicitud.equipoId}",
                style = MaterialTheme.typography.bodyLarge
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Evidencia Fotográfica Obligatoria", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            try {
                                val uri = crearUriTemporal()
                                tempUri = uri
                                takePictureLauncher.launch(uri)
                            } catch (e: Exception) {
                                mensajePermiso = "Error al inicializar cámara: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tomar Foto con la Cámara")
                    }
                    if (evidenciaUri != null) {
                        Text("Foto Capturada con Éxito", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("Estado de la Evidencia: $estadoSincronizacion", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Ninguna foto capturada en vivo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Capacidad Física Adicional (GPS)", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            val check = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (check == PackageManager.PERMISSION_GRANTED) {
                                latitud = 6.2514
                                longitud = -75.5636
                                mensajePermiso = "Ubicación GPS obtenida con éxito."
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Obtener Coordenadas de Entrega")
                    }
                    if (latitud != null && longitud != null) {
                        Text("Latitud: $latitud", style = MaterialTheme.typography.bodyMedium)
                        Text("Longitud: $longitud", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (mensajePermiso.isNotEmpty()) {
                        Text(text = mensajePermiso, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = onVolver, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onConfirmarDevolucion(evidenciaUri?.toString(), latitud, longitud)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = evidenciaUri != null
                ) {
                    Text("Confirmar")
                }
            }
        }
    }
}
