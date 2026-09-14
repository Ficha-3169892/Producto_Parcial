package com.example.ctma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actividades")
data class ActividadEntity(
    @PrimaryKey val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaLimite: String,
    val estado: String,
    val ultimaActualizacion: Long = System.currentTimeMillis()
)
