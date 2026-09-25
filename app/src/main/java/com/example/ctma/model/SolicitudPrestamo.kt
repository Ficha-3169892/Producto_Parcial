package com.example.ctma.model

data class SolicitudPrestamo(
    val id: Int,
    val equipoId: Int,
    val ambienteDestino: String,
    val proposito: String,
    val duracionHoras: Int,
    val estado: EstadoSolicitud,
    val evidenciaUri: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val estadoEvidencia: String? = null

)