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

    /**
     * Implementación Fake dinámica para probar lógica de negocio sin dependencias de Android.
     */
    private class FakeTestPrestamoRepository : PrestamoRepository {
        val equipos = mutableListOf(
            Equipo(1, "Multímetro", "Electrónica", EstadoEquipo.DISPONIBLE),
            Equipo(2, "Osciloscopio", "Electrónica", EstadoEquipo.PRESTADO)
        )
        val solicitudes = mutableListOf<SolicitudPrestamo>()

        override fun obtenerEquipos(): List<Equipo> = equipos
        override fun obtenerEquipo(id: Int): Equipo? = equipos.find { it.id == id }
        override fun obtenerSolicitudes(): List<SolicitudPrestamo> = solicitudes
        override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = solicitudes.find { it.id == id }
        
        override fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> {
            val equipo = obtenerEquipo(solicitud.equipoId) ?: return Result.failure(Exception("Equipo no existe"))
            if (equipo.estado != EstadoEquipo.DISPONIBLE) return Result.failure(Exception("No disponible"))
            
            val nueva = solicitud.copy(id = solicitudes.size + 1)
            solicitudes.add(nueva)
            equipos[equipos.indexOf(equipo)] = equipo.copy(estado = EstadoEquipo.PRESTADO)
            return Result.success(Unit)
        }

        override fun cancelarSolicitud(id: Int): Result<Unit> {
            val index = solicitudes.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("No encontrada"))
            
            val sol = solicitudes[index]
            solicitudes[index] = sol.copy(estado = EstadoSolicitud.CANCELADA)
            val eqIndex = equipos.indexOfFirst { it.id == sol.equipoId }
            if (eqIndex != -1) {
                equipos[eqIndex] = equipos[eqIndex].copy(estado = EstadoEquipo.DISPONIBLE)
            }
            return Result.success(Unit)
        }

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
    fun `cancelarSolicitud cambia estado a CANCELADA y libera el equipo`() {
        // Arrange: Creamos una solicitud exitosa para el equipo 1
        val solicitud = SolicitudPrestamo(0, 1, "A-1", "Test", 1, EstadoSolicitud.SOLICITADA)
        repository.crearSolicitud(solicitud)
        val idGenerado = repository.obtenerSolicitudes().first().id

        // Act
        val resultado = repository.cancelarSolicitud(idGenerado)

        // Assert
        assertTrue(resultado.isSuccess)
        assertEquals(EstadoSolicitud.CANCELADA, repository.obtenerSolicitud(idGenerado)?.estado)
        assertEquals(EstadoEquipo.DISPONIBLE, repository.obtenerEquipo(1)?.estado)
    }

    @Test
    fun `crearSolicitud falla si el equipo ya esta PRESTADO`() {
        // Arrange: El equipo 2 ya está PRESTADO en el Fake
        val solicitud = SolicitudPrestamo(0, 2, "A-2", "Uso rudo", 2, EstadoSolicitud.SOLICITADA)

        // Act
        val resultado = repository.crearSolicitud(solicitud)

        // Assert
        assertTrue(resultado.isFailure)
        assertEquals("No disponible", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `registrarDevolucion falla con un ID de solicitud inexistente`() {
        // Act: Intentamos devolver una solicitud con ID 999 que no existe
        val resultado = repository.registrarDevolucion(999, "uri", 0.0, 0.0)

        // Assert
        assertTrue(resultado.isFailure)
        assertEquals("No existe solicitud", resultado.exceptionOrNull()?.message)
    }
}
