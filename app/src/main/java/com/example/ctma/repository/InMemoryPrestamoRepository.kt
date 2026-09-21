package com.example.ctma.repository

import com.example.ctma.model.Equipo
import com.example.ctma.model.EstadoEquipo
import com.example.ctma.model.EstadoSolicitud
import com.example.ctma.model.SolicitudPrestamo

class InMemoryPrestamoRepository : PrestamoRepository {

    private val equipos = mutableListOf(
        Equipo(
            id = 1,
            nombre = "Multímetro Digital",
            categoria = "Electrónica",
            estado = EstadoEquipo.DISPONIBLE
        ),
        Equipo(
            id = 2,
            nombre = "Osciloscopio",
            categoria = "Electrónica",
            estado = EstadoEquipo.DISPONIBLE
        ),
        Equipo(
            id = 3,
            nombre = "Fuente de Alimentación",
            categoria = "Electrónica",
            estado = EstadoEquipo.PRESTADO
        )
    )

    private val solicitudes = mutableListOf<SolicitudPrestamo>()

    private var siguienteIdSolicitud = 1

    override fun obtenerEquipos(): List<Equipo> {
        return equipos.toList()
    }

    override fun obtenerEquipo(id: Int): Equipo? {
        return equipos.find { it.id == id }
    }

    override fun obtenerSolicitudes(): List<SolicitudPrestamo> {
        return solicitudes.toList()
    }

    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? {
        return solicitudes.find { it.id == id }
    }

    override fun crearSolicitud(
        solicitud: SolicitudPrestamo
    ): Result<Unit> {

        val equipo = obtenerEquipo(solicitud.equipoId)

        // Validar que el equipo exista
        if (equipo == null) {
            return Result.failure(
                IllegalArgumentException(
                    "El equipo no existe"
                )
            )
        }

        // Validar disponibilidad
        if (equipo.estado != EstadoEquipo.DISPONIBLE) {
            return Result.failure(
                IllegalStateException(
                    "El equipo no está disponible"
                )
            )
        }

        // Evitar solicitudes duplicadas
        val solicitudActiva = solicitudes.any {
            it.equipoId == solicitud.equipoId &&
                    (
                            it.estado == EstadoSolicitud.SOLICITADA ||
                                    it.estado == EstadoSolicitud.APROBADA
                            )
        }

        if (solicitudActiva) {
            return Result.failure(
                IllegalStateException(
                    "Ya existe una solicitud activa para este equipo"
                )
            )
        }

        // Crear una solicitud con un ID único
        val nuevaSolicitud = solicitud.copy(
            id = siguienteIdSolicitud,
            estado = EstadoSolicitud.SOLICITADA
        )

        siguienteIdSolicitud++

        solicitudes.add(nuevaSolicitud)

        // El equipo deja de estar disponible
        actualizarEstadoEquipo(
            equipoId = equipo.id,
            nuevoEstado = EstadoEquipo.PRESTADO
        )

        return Result.success(Unit)
    }

    override fun cancelarSolicitud(
        id: Int
    ): Result<Unit> {

        val indice = solicitudes.indexOfFirst {
            it.id == id
        }

        // Validar que la solicitud exista
        if (indice == -1) {
            return Result.failure(
                IllegalArgumentException(
                    "La solicitud no existe"
                )
            )
        }

        val solicitud = solicitudes[indice]

        // Solo se puede cancelar una solicitud SOLICITADA
        if (solicitud.estado != EstadoSolicitud.SOLICITADA) {
            return Result.failure(
                IllegalStateException(
                    "Solo se pueden cancelar solicitudes solicitadas"
                )
            )
        }

        // Mantener la solicitud, cambiando su estado
        solicitudes[indice] = solicitud.copy(
            estado = EstadoSolicitud.CANCELADA
        )

        // El equipo vuelve a estar disponible
        actualizarEstadoEquipo(
            equipoId = solicitud.equipoId,
            nuevoEstado = EstadoEquipo.DISPONIBLE
        )

        return Result.success(Unit)
    }

    override fun registrarDevolucion(
        id: Int,
        evidenciaUri: String?,
        latitud: Double?,
        longitud: Double?
    ): Result<Unit> {
        val indice = solicitudes.indexOfFirst { it.id == id }
        if (indice == -1) {
            return Result.failure(IllegalArgumentException("La solicitud no existe"))
        }

        val solicitud = solicitudes[indice]
        solicitudes[indice] = solicitud.copy(
            estado = EstadoSolicitud.DEVUELTA,
            evidenciaUri = evidenciaUri,
            latitud = latitud,
            longitud = longitud,
            estadoEvidencia = "SINCRONIZADA"
        )

        actualizarEstadoEquipo(
            equipoId = solicitud.equipoId,
            nuevoEstado = EstadoEquipo.DISPONIBLE
        )

        return Result.success(Unit)
    }

    private fun actualizarEstadoEquipo(
        equipoId: Int,
        nuevoEstado: EstadoEquipo
    ) {

        val indice = equipos.indexOfFirst {
            it.id == equipoId
        }

        if (indice != -1) {

            equipos[indice] = equipos[indice].copy(
                estado = nuevoEstado
            )
        }
    }
}