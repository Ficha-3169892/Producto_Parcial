package com.example.ctma.data.mapper

import com.example.ctma.data.local.ActividadEntity
import com.example.ctma.data.remote.ActividadDto
import com.example.ctma.domain.ActividadFormativa
import org.junit.Assert.assertEquals
import org.junit.Test

class ActividadMapperTest {

    @Test
    fun `dtoToEntity transforma dto correctamente manejando valores nulos`() {
        val dto = ActividadDto(
            id = 10,
            titulo = "Taller Retrofit",
            descripcion = null,
            fechaLimite = null,
            estado = null
        )

        val entity = ActividadMapper.dtoToEntity(dto)

        assertEquals(10, entity.id)
        assertEquals("Taller Retrofit", entity.titulo)
        assertEquals("Sin descripción", entity.descripcion)
        assertEquals("Sin fecha", entity.fechaLimite)
        assertEquals("PENDIENTE", entity.estado)
    }

    @Test
    fun `entityToDomain mapea entity a dominio de forma identica`() {
        val entity = ActividadEntity(
            id = 5,
            titulo = "Evaluación Room",
            descripcion = "Persistencia local",
            fechaLimite = "2026-10-01",
            estado = "EN_PROGRESO"
        )

        val domain = ActividadMapper.entityToDomain(entity)

        assertEquals(5, domain.id)
        assertEquals("Evaluación Room", domain.titulo)
        assertEquals("Persistencia local", domain.descripcion)
        assertEquals("2026-10-01", domain.fechaLimite)
        assertEquals("EN_PROGRESO", domain.estado)
    }
}
