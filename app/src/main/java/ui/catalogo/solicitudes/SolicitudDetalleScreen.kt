package com.example.ctma.ui.catalogo.solicitudes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ctma.model.SolicitudPrestamo

@Composable
fun SolicitudDetalleScreen(
    solicitud: SolicitudPrestamo,
    onVolver: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Detalle de solicitud",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Solicitud #${solicitud.id}",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Equipo ID: ${solicitud.equipoId}"
        )

        Text(
            text = "Ambiente de destino: ${solicitud.ambienteDestino}"
        )

        Text(
            text = "Propósito: ${solicitud.proposito}"
        )

        Text(
            text = "Duración: ${solicitud.duracionHoras} horas"
        )

        Text(
            text = "Estado: ${solicitud.estado}"
        )

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}