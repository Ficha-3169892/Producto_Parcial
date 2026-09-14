package com.example.ctma.data.mapper

import com.example.ctma.data.local.ActividadEntity
import com.example.ctma.data.remote.ActividadDto
import com.example.ctma.domain.ActividadFormativa

object ActividadMapper {

    fun dtoToEntity(dto: ActividadDto): ActividadEntity {
        return ActividadEntity(
            id = dto.id,
            titulo = dto.titulo,
            descripcion = dto.descripcion ?: "Sin descripción",
            fechaLimite = dto.fechaLimite ?: "Sin fecha",
            estado = dto.estado ?: "PENDIENTE"
        )
    }

    fun entityToDomain(entity: ActividadEntity): ActividadFormativa {
        return ActividadFormativa(
            id = entity.id,
            titulo = entity.titulo,
            descripcion = entity.descripcion,
            fechaLimite = entity.fechaLimite,
            estado = entity.estado
        )
    }

    fun dtoListToEntityList(dtos: List<ActividadDto>): List<ActividadEntity> {
        return dtos.map { dtoToEntity(it) }
    }

    fun entityListToDomainList(entities: List<ActividadEntity>): List<ActividadFormativa> {
        return entities.map { entityToDomain(it) }
    }
}
