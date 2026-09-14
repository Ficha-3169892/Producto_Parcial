package com.example.ctma.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ActividadApi {
    @GET("v1/actividades")
    suspend fun listarActividades(): Response<List<ActividadDto>>

    @GET("v1/actividades/{id}")
    suspend fun obtenerActividad(@Path("id") id: Int): Response<ActividadDto>
}
