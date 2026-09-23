package com.example.ctma.ui.catalogo.solicitudes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ctma.model.SolicitudPrestamo
import com.example.ctma.ui.components.ImagenEvidencia

@Composable
fun SolicitudDetalleScreen(
    solicitud: SolicitudPrestamo,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Detalle de Solicitud",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Solicitud #${solicitud.id}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(text = "Equipo ID: ${solicitud.equipoId}")
                Text(text = "Ambiente de Destino: ${solicitud.ambienteDestino}")
                Text(text = "Propósito: ${solicitud.proposito}")
                Text(text = "Duración: ${solicitud.duracionHoras} horas")
                Text(text = "Estado: ${solicitud.estado}")
            }
        }

        // Sección de Evidencia Fotográfica
        if (!solicitud.evidenciaUri.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📷 Evidencia Fotográfica",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ImagenEvidencia(
                        uriString = solicitud.evidenciaUri,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!solicitud.estadoEvidencia.isNullOrEmpty()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Sincronización: ${solicitud.estadoEvidencia}") }
                        )
                    }
                }
            }
        }

        // Sección de Georreferenciación (GPS)
        if (solicitud.latitud != null && solicitud.longitud != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📍 Georreferenciación (GPS)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Latitud: ${solicitud.latitud}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Longitud: ${solicitud.longitud}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
