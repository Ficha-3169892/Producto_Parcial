package com.example.ctma.repository

import com.example.ctma.model.Equipo
import com.example.ctma.model.SolicitudPrestamo

interface PrestamoRepository {

    fun obtenerEquipos(): List<Equipo>

    fun obtenerEquipo(id: Int): Equipo?

    fun obtenerSolicitudes(): List<SolicitudPrestamo>

    fun obtenerSolicitud(id: Int): SolicitudPrestamo?

    fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit>

    fun cancelarSolicitud(id: Int): Result<Unit>

    fun registrarDevolucion(id: Int, evidenciaUri: String?, latitud: Double?, longitud: Double?): Result<Unit>
}