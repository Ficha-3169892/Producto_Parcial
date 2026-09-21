package com.example.ctma.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ctma.model.Equipo
import com.example.ctma.ui.catalogo.CatalogoScreen
import com.example.ctma.ui.catalogo.solicitud.SolicitudScreen
import com.example.ctma.ui.catalogo.solicitudes.SolicitudDetalleScreen
import com.example.ctma.ui.catalogo.solicitudes.SolicitudesScreen
import com.example.ctma.ui.catalogo.solicitudes.DevolucionScreen
import com.example.ctma.viewmodel.PrestamoViewModel

@Composable
fun AppNavigation(
    viewModel: PrestamoViewModel
) {

    /*
     * 0 = Catálogo
     * 1 = Mis solicitudes
     * 2 = Crear solicitud
     * 3 = Detalle de solicitud
     */

    var pantallaActual by remember {
        mutableIntStateOf(0)
    }

    var equipoSeleccionado by remember {
        mutableStateOf<Equipo?>(null)
    }

    /*
     * Guardamos únicamente el ID de la solicitud.
     */
    var solicitudSeleccionadaId by remember {
        mutableStateOf<Int?>(null)
    }

    /*
     * Observamos el estado del ViewModel.
     */
    val uiState by viewModel.uiState.collectAsState()

    when (pantallaActual) {

        // ==============================
        // CATÁLOGO
        // ==============================

        0 -> {

            CatalogoScreen(
                equipos = uiState.equipos,

                onEquipoSeleccionado = { equipo ->

                    equipoSeleccionado = equipo
                    pantallaActual = 2
                },

                onMisSolicitudes = {

                    pantallaActual = 1
                }
            )
        }

        // ==============================
        // MIS SOLICITUDES
        // ==============================

        1 -> {

            SolicitudesScreen(
                solicitudes = uiState.solicitudes,

                onCancelarSolicitud = { id ->

                    viewModel.cancelarSolicitud(id)
                },

                onSolicitudSeleccionada = { solicitudId ->

                    solicitudSeleccionadaId = solicitudId
                    pantallaActual = 3
                },

                onRegistrarDevolucion = { solicitudId ->
                    solicitudSeleccionadaId = solicitudId
                    pantallaActual = 4
                },

                onVolverCatalogo = {

                    pantallaActual = 0
                }
            )
        }

        // ==============================
        // CREAR SOLICITUD
        // ==============================

        2 -> {

            val equipo = equipoSeleccionado

            if (equipo != null) {

                SolicitudScreen(
                    equipo = equipo,

                    onSolicitudCreada = { solicitud ->

                        val resultado = viewModel.crearSolicitud(solicitud)

                        if (resultado.isSuccess) {

                            equipoSeleccionado = null
                            pantallaActual = 1
                        }

                        resultado
                    }
                )

            } else {

                pantallaActual = 0
            }
        }

        // ==============================
        // DETALLE DE SOLICITUD
        // ==============================

        3 -> {

            val solicitudId = solicitudSeleccionadaId

            if (solicitudId != null) {

                val solicitud = viewModel.obtenerSolicitud(solicitudId)

                if (solicitud != null) {

                    SolicitudDetalleScreen(
                        solicitud = solicitud,

                        onVolver = {

                            solicitudSeleccionadaId = null
                            pantallaActual = 1
                        }
                    )

                } else {

                    /*
                     * El ID no existe.
                     * Regresamos a Mis solicitudes
                     * sin cerrar la aplicación.
                     */

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Solicitud no encontrada",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Button(
                            onClick = {

                                solicitudSeleccionadaId = null
                                pantallaActual = 1
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {

                            Text(
                                text = "Volver"
                            )
                        }
                    }
                }

            } else {

                pantallaActual = 1
            }
        }

        // ==============================
        // REGISTRAR DEVOLUCIÓN
        // ==============================

        4 -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val solicitudId = solicitudSeleccionadaId
            if (solicitudId != null) {
                val solicitud = viewModel.obtenerSolicitud(solicitudId)
                if (solicitud != null) {
                    DevolucionScreen(
                        solicitud = solicitud,
                        onConfirmarDevolucion = { uri, lat, lon ->
                            viewModel.registrarDevolucion(context, solicitudId, uri, lat, lon)
                            solicitudSeleccionadaId = null
                            pantallaActual = 1
                        },
                        onVolver = {
                            solicitudSeleccionadaId = null
                            pantallaActual = 1
                        }
                    )
                } else {
                    pantallaActual = 1
                }
            } else {
                pantallaActual = 1
            }
        }
    }
}