package com.example.ctma.ui.catalogo.solicitudes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ctma.model.EstadoSolicitud
import com.example.ctma.model.SolicitudPrestamo

@Composable
fun SolicitudesScreen(
    solicitudes: List<SolicitudPrestamo>,
    onCancelarSolicitud: (Int) -> Unit,
    onSolicitudSeleccionada: (Int) -> Unit,
    onRegistrarDevolucion: (Int) -> Unit,
    onVolverCatalogo: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Mis solicitudes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = {
                onVolverCatalogo()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Volver al catálogo")
        }

        if (solicitudes.isEmpty()) {

            Text(
                text = "No hay solicitudes registradas.",
                style = MaterialTheme.typography.bodyLarge
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = solicitudes,
                    key = { solicitud ->
                        solicitud.id
                    }
                ) { solicitud ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Text(
                                text = "Solicitud #${solicitud.id}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "Equipo ID: ${solicitud.equipoId}"
                            )

                            Text(
                                text = "Ambiente: ${solicitud.ambienteDestino}"
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        onSolicitudSeleccionada(solicitud.id)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ver detalle")
                                }

                                if (solicitud.estado == EstadoSolicitud.SOLICITADA) {

                                    Button(
                                        onClick = {
                                            onCancelarSolicitud(solicitud.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancelar")
                                    }

                                    Button(
                                        onClick = {
                                            onRegistrarDevolucion(solicitud.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Devolver")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}