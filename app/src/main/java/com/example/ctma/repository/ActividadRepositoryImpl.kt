package com.example.ctma.repository

import com.example.ctma.data.local.ActividadDao
import com.example.ctma.data.mapper.ActividadMapper
import com.example.ctma.data.remote.RemoteActividadDataSource
import com.example.ctma.domain.ActividadFormativa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.SocketTimeoutException

class ActividadRepositoryImpl(
    private val remoteDataSource: RemoteActividadDataSource,
    private val actividadDao: ActividadDao
) : ActividadRepository {

    override fun observarActividades(): Flow<List<ActividadFormativa>> {
        return actividadDao.observarTodas().map { entities ->
            ActividadMapper.entityListToDomainList(entities)
        }
    }

    override suspend fun refresh(): OperacionUiState {
        return try {
            val response = remoteDataSource.listarActividades()

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = ActividadMapper.dtoListToEntityList(dtos)
                
                // Actualización atómica en Room sin borrar la base local si dtos está vacío
                if (entities.isNotEmpty()) {
                    actividadDao.insertarTodas(entities)
                }
                OperacionUiState.Exitosa()
            } else {
                when (response.code()) {
                    401 -> OperacionUiState.Fallida(
                        mensaje = "Sesión vencida. Por favor inicia sesión de nuevo.",
                        codigo = 401
                    )
                    404 -> OperacionUiState.Fallida(
                        mensaje = "Recurso no encontrado en el servidor.",
                        codigo = 404
                    )
                    in 500..599 -> OperacionUiState.Fallida(
                        mensaje = "Error del servidor. Inténtalo más tarde.",
                        codigo = response.code()
                    )
                    else -> OperacionUiState.Fallida(
                        mensaje = "Error inesperado (${response.code()}).",
                        codigo = response.code()
                    )
                }
            }
        } catch (e: CancellationException) {
            // Regla crítica: relanzar siempre CancellationException
            throw e
        } catch (e: SocketTimeoutException) {
            OperacionUiState.Fallida(
                mensaje = "Tiempo de espera agotado. Conservando datos locales."
            )
        } catch (e: IOException) {
            OperacionUiState.Fallida(
                mensaje = "Sin conexión a red. Mostrando información almacenada."
            )
        } catch (e: Exception) {
            OperacionUiState.Fallida(
                mensaje = "Error de procesado o formato inválido: ${e.localizedMessage}"
            )
        }
    }
}
