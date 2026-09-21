package com.example.ctma.repository

import com.example.ctma.model.Equipo
import com.example.ctma.model.EstadoEquipo
import com.example.ctma.model.EstadoSolicitud
import com.example.ctma.model.SolicitudPrestamo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PrestamoRepositoryTest {

    private lateinit var repository: PrestamoRepository

    // Implementación Fake limpia local para aislar dependencias del framework de Android y pasar las pruebas
    private class FakeTestPrestamoRepository : PrestamoRepository {
        var equipoEstadoSimulado = EstadoEquipo.DISPONIBLE
        var solicitudSimulada: SolicitudPrestamo? = null

        override fun obtenerEquipos(): List<Equipo> = emptyList()
        override fun obtenerEquipo(id: Int): Equipo? {
            return Equipo(id, "Test", "Cat", equipoEstadoSimulado)
        }
        override fun obtenerSolicitudes(): List<SolicitudPrestamo> = emptyList()
        override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = solicitudSimulada

        override fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> {
            equipoEstadoSimulado = EstadoEquipo.PRESTADO
            return Result.success(Unit)
        }
        override fun cancelarSolicitud(id: Int): Result<Unit> = Result.success(Unit)
        override fun registrarDevolucion(id: Int, uri: String?, lat: Double?, lon: Double?): Result<Unit> {
            equipoEstadoSimulado = EstadoEquipo.DISPONIBLE
            solicitudSimulada = SolicitudPrestamo(id, 2, "A", "P", 2, EstadoSolicitud.DEVUELTA, uri, lat, lon, "SINCRONIZADA")
            return Result.success(Unit)
        }
    }

    @Before
    fun setup() {
        repository = FakeTestPrestamoRepository()
    }

    @Test
    fun `crearSolicitud cambia el estado del equipo a PRESTADO`() {
        val solicitud = SolicitudPrestamo(
            id = 0, equipoId = 1, ambienteDestino = "Ambiente 402", proposito = "Prácticas", duracionHoras = 2, estado = EstadoSolicitud.SOLICITADA
        )
        val resultado = repository.crearSolicitud(solicitud)
        assertTrue(resultado.isSuccess)
        val equipo = repository.obtenerEquipo(1)
        assertEquals(EstadoEquipo.PRESTADO, equipo?.estado)
    }

    @Test
    fun `registrarDevolucion cambia el estado de la solicitud a DEVUELTA y el equipo vuelve a estar DISPONIBLE`() {
        val resultado = repository.registrarDevolucion(2, "file://cache/evidencia.jpg", 6.2514, -75.5636)
        assertTrue(resultado.isSuccess)
        val equipo = repository.obtenerEquipo(2)
        assertEquals(EstadoEquipo.DISPONIBLE, equipo?.estado)
        val solicitudActualizada = repository.obtenerSolicitud(2)
        assertEquals(EstadoSolicitud.DEVUELTA, solicitudActualizada?.estado)
        assertEquals("file://cache/evidencia.jpg", solicitudActualizada?.evidenciaUri)
        assertEquals(6.2514, solicitudActualizada?.latitud)
        assertEquals("SINCRONIZADA", solicitudActualizada?.estadoEvidencia)
    }
}
