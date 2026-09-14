package com.example.ctma.repository

import com.example.ctma.domain.ActividadFormativa
import kotlinx.coroutines.flow.Flow

sealed interface OperacionUiState {
    data object Inactiva : OperacionUiState
    data object EnCurso : OperacionUiState
    data class Exitosa(val timestamp: Long = System.currentTimeMillis()) : OperacionUiState
    data class Fallida(val mensaje: String, val codigo: Int? = null) : OperacionUiState
}

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data class Contenido(val actividades: List<ActividadFormativa>) : ListadoUiState
    data object Vacio : ListadoUiState
    data class Error(val mensaje: String) : ListadoUiState
}

interface ActividadRepository {
    fun observarActividades(): Flow<List<ActividadFormativa>>
    suspend fun refresh(): OperacionUiState
}
