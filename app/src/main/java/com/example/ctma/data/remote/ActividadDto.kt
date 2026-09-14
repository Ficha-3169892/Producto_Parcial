package com.example.ctma.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActividadDto(
    @SerialName("id") val id: Int,
    @SerialName("titulo") val titulo: String,
    @SerialName("descripcion") val descripcion: String? = null,
    @SerialName("fechaLimite") val fechaLimite: String? = null,
    @SerialName("estado") val estado: String? = null
)
