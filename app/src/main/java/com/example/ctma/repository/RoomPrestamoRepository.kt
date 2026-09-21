package com.example.ctma.repository

import com.example.ctma.data.local.EquipoRoomEntity
import com.example.ctma.data.local.PrestamoDao
import com.example.ctma.data.local.SolicitudRoomEntity
import com.example.ctma.model.Equipo
import com.example.ctma.model.EstadoEquipo
import com.example.ctma.model.EstadoSolicitud
import com.example.ctma.model.SolicitudPrestamo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomPrestamoRepository(
    private val prestamoDao: PrestamoDao
) : PrestamoRepository {

    suspend fun inicializarEquiposSiEsNecesario() = withContext(Dispatchers.IO) {
        val existentes = prestamoDao.obtenerTodosLosEquipos()
        if (existentes.isEmpty()) {
            val iniciales = listOf(
                EquipoRoomEntity(1, "Multímetro Digital", "Electrónica", "DISPONIBLE"),
                EquipoRoomEntity(2, "Osciloscopio", "Electrónica", "DISPONIBLE"),
                EquipoRoomEntity(3, "Fuente de Alimentación", "Electrónica", "PRESTADO")
            )
            prestamoDao.insertarEquipos(iniciales)
        }
    }

    suspend fun obtenerEquiposAsync(): List<Equipo> = withContext(Dispatchers.IO) {
        prestamoDao.obtenerTodosLosEquipos().map {
            Equipo(it.id, it.nombre, it.categoria, EstadoEquipo.valueOf(it.estado))
        }
    }

    suspend fun obtenerEquipoAsync(id: Int): Equipo? = withContext(Dispatchers.IO) {
        prestamoDao.obtenerEquipoPorId(id)?.let {
            Equipo(it.id, it.nombre, it.categoria, EstadoEquipo.valueOf(it.estado))
        }
    }

    suspend fun obtenerSolicitudesAsync(): List<SolicitudPrestamo> = withContext(Dispatchers.IO) {
        prestamoDao.obtenerTodasLasSolicitudes().map {
            SolicitudPrestamo(
                it.id, it.equipoId, it.ambienteDestino, it.proposito, it.duracionHoras,
                EstadoSolicitud.valueOf(it.estado), it.evidenciaUri, it.latitud, it.longitud, it.estadoEvidencia
            )
        }
    }

    suspend fun obtenerSolicitudAsync(id: Int): SolicitudPrestamo? = withContext(Dispatchers.IO) {
        prestamoDao.obtenerSolicitudPorId(id)?.let {
            SolicitudPrestamo(
                it.id, it.equipoId, it.ambienteDestino, it.proposito, it.duracionHoras,
                EstadoSolicitud.valueOf(it.estado), it.evidenciaUri, it.latitud, it.longitud, it.estadoEvidencia
            )
        }
    }

    suspend fun crearSolicitudAsync(solicitud: SolicitudPrestamo): Result<Unit> = withContext(Dispatchers.IO) {
        val equipo = prestamoDao.obtenerEquipoPorId(solicitud.equipoId)
            ?: return@withContext Result.failure<Unit>(IllegalArgumentException("El equipo no existe"))

        if (equipo.estado != "DISPONIBLE") {
            return@withContext Result.failure<Unit>(IllegalStateException("El equipo no está disponible"))
        }

        val todas = prestamoDao.obtenerTodasLasSolicitudes()
        val duplicada = todas.any { it.equipoId == solicitud.equipoId && (it.estado == "SOLICITADA" || it.estado == "APROBADA") }
        if (duplicada) {
            return@withContext Result.failure<Unit>(IllegalStateException("Ya existe una solicitud activa para este equipo"))
        }

        prestamoDao.insertarSolicitud(
            SolicitudRoomEntity(
                equipoId = solicitud.equipoId,
                ambienteDestino = solicitud.ambienteDestino,
                proposito = solicitud.proposito,
                duracionHoras = solicitud.duracionHoras,
                estado = "SOLICITADA"
            )
        )

        prestamoDao.insertarEquipo(equipo.copy(estado = "PRESTADO"))
        Result.success(Unit)
    }

    suspend fun cancelarSolicitudAsync(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val sol = prestamoDao.obtenerSolicitudPorId(id)
            ?: return@withContext Result.failure<Unit>(IllegalArgumentException("La solicitud no existe"))

        if (sol.estado != "SOLICITADA") {
            return@withContext Result.failure<Unit>(IllegalStateException("Solo se pueden cancelar solicitudes solicitadas"))
        }

        prestamoDao.insertarSolicitud(sol.copy(estado = "CANCELADA"))
        prestamoDao.obtenerEquipoPorId(sol.equipoId)?.let {
            prestamoDao.insertarEquipo(it.copy(estado = "DISPONIBLE"))
        }
        Result.success(Unit)
    }

    suspend fun registrarDevolucionAsync(id: Int, evidenciaUri: String?, latitud: Double?, longitud: Double?): Result<Unit> = withContext(Dispatchers.IO) {
        val sol = prestamoDao.obtenerSolicitudPorId(id)
            ?: return@withContext Result.failure<Unit>(IllegalArgumentException("La solicitud no existe"))

        prestamoDao.insertarSolicitud(
            sol.copy(
                estado = "DEVUELTA",
                evidenciaUri = evidenciaUri,
                latitud = latitud,
                longitud = longitud,
                estadoEvidencia = "SINCRONIZADA"
            )
        )

        prestamoDao.obtenerEquipoPorId(sol.equipoId)?.let {
            prestamoDao.insertarEquipo(it.copy(estado = "DISPONIBLE"))
        }
        Result.success(Unit)
    }

    // Sobreescrituras heredadas obligatorias (fines síncronos legados)
    override fun obtenerEquipos(): List<Equipo> = emptyList()
    override fun obtenerEquipo(id: Int): Equipo? = null
    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = emptyList()
    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = null
    override fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> = Result.success(Unit)
    override fun cancelarSolicitud(id: Int): Result<Unit> = Result.success(Unit)
    override fun registrarDevolucion(id: Int, evidenciaUri: String?, latitud: Double?, longitud: Double?): Result<Unit> = Result.success(Unit)
}
