package com.example.ctma.data.remote

import retrofit2.Response

class RemoteActividadDataSource(
    private val api: ActividadApi
) {
    suspend fun listarActividades(): Response<List<ActividadDto>> {
        return api.listarActividades()
    }

    suspend fun obtenerActividad(id: Int): Response<ActividadDto> {
        return api.obtenerActividad(id)
    }
}
