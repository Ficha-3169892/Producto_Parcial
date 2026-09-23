package com.example.ctma.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ctma.data.local.AppDatabase
import com.example.ctma.model.Equipo
import com.example.ctma.model.SolicitudPrestamo
import com.example.ctma.repository.RoomPrestamoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensaje: String? = null
)

class PrestamoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoomPrestamoRepository

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = RoomPrestamoRepository(database.prestamoDao())
        
        viewModelScope.launch {
            repository.inicializarEquiposSiEsNecesario()
            actualizarEstado()
        }
    }

    fun obtenerEquipos(): List<Equipo> {
        return _uiState.value.equipos
    }

    fun obtenerSolicitudes(): List<SolicitudPrestamo> {
        return _uiState.value.solicitudes
    }

    fun obtenerSolicitud(id: Int): SolicitudPrestamo? {
        return _uiState.value.solicitudes.find { it.id == id }
    }

    fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> {
        val resultado = repository.crearSolicitud(solicitud)
        actualizarEstado()
        return resultado
    }

    fun cancelarSolicitud(id: Int): Result<Unit> {
        val resultado = repository.cancelarSolicitud(id)
        actualizarEstado()
        return resultado
    }

    fun registrarDevolucion(
        context: android.content.Context,
        id: Int,
        evidenciaUri: String?,
        latitud: Double?,
        longitud: Double?
    ): Result<Unit> {
        val resultado = repository.registrarDevolucion(id, evidenciaUri, latitud, longitud)
        actualizarEstado()
        if (resultado.isSuccess) {
            mostrarNotificacionContextual(context, id)
        }
        return resultado
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun mostrarNotificacionContextual(context: android.content.Context, solicitudId: Int) {
        val channelId = "devoluciones_channel"
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            channelId,
            "Devoluciones de Equipos",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones de devoluciones procesadas"
        }
        notificationManager.createNotificationChannel(channel)
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Devolución Exitosa")
            .setContentText("La devolución de la solicitud #$solicitudId ha sido registrada y sincronizada.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        notificationManager.notify(solicitudId, builder.build())
    }

    private fun actualizarEstado() {
        viewModelScope.launch {
            val eqs = repository.obtenerEquiposAsync()
            val sols = repository.obtenerSolicitudesAsync()
            withContext(Dispatchers.Main) {
                _uiState.value = PrestamoUiState(equipos = eqs, solicitudes = sols)
            }
        }
    }
}
