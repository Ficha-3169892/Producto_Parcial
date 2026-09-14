package com.example.ctma.repository

import com.example.ctma.auth.SessionTokenProvider
import com.example.ctma.data.local.ActividadDao
import com.example.ctma.data.local.ActividadEntity
import com.example.ctma.data.remote.ActividadApi
import com.example.ctma.data.remote.NetworkModule
import com.example.ctma.data.remote.RemoteActividadDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeActividadDao : ActividadDao {
    private val db = mutableListOf<ActividadEntity>()
    private val flow = MutableStateFlow<List<ActividadEntity>>(emptyList())

    override fun observarTodas(): Flow<List<ActividadEntity>> = flow

    override suspend fun obtenerPorId(id: Int): ActividadEntity? {
        return db.find { it.id == id }
    }

    override suspend fun insertarTodas(actividades: List<ActividadEntity>) {
        db.removeAll { existing -> actividades.any { it.id == existing.id } }
        db.addAll(actividades)
        flow.value = db.toList()
    }

    override suspend fun limpiarTodas() {
        db.clear()
        flow.value = emptyList()
    }
}

class ActividadRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: ActividadApi
    private lateinit var dao: FakeActividadDao
    private lateinit var repository: ActividadRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val tokenProvider = SessionTokenProvider()
        val okHttpClient = NetworkModule.crearOkHttpClient(tokenProvider)
        api = NetworkModule.crearActividadApi(mockWebServer.url("/").toString(), okHttpClient)

        dao = FakeActividadDao()
        val remoteDataSource = RemoteActividadDataSource(api)
        repository = ActividadRepositoryImpl(remoteDataSource, dao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // CA-07: Actualización de datos en Room
    @Test
    fun `CA-07 - 200 refresh reemplaza los datos antiguos en la base de datos local`() = runTest {
        // Insertar un dato antiguo en el FakeDao
        dao.insertarTodas(listOf(ActividadEntity(1, "Titulo Viejo", "Desc", "2026-09-01", "PENDIENTE")))

        // Simular que el servidor envía el mismo ID pero con datos actualizados
        val jsonResponse = """
            [
                {"id": 1, "titulo": "Titulo Nuevo", "descripcion": "Desc", "fechaLimite": "2026-09-01", "estado": "HECHO"}
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        repository.refresh()

        // Verificar que el repositorio guardó los datos nuevos sobreescribiendo los viejos
        val guardado = dao.obtenerPorId(1)
        assertEquals("Titulo Nuevo", guardado?.titulo)
        assertEquals("HECHO", guardado?.estado)
    }

    // CA-08: 404 Endpoint no encontrado
    @Test
    fun `CA-08 - 404 ruta no encontrada retorna estado Fallida`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(404, fallida.codigo)
    }

    // CA-09: JSON con formato incorrecto
    @Test
    fun `CA-09 - 200 con JSON malformado falla al procesar y retorna Fallida`() = runTest {
        // Se envía un JSON donde el ID es un texto en lugar de un número, provocando error de parseo
        val jsonInvalido = """
            [
                {"id": "uno_en_texto", "titulo": "Actividad 1", "descripcion": "Desc 1", "fechaLimite": "2026-09-30", "estado": "PENDIENTE"}
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonInvalido))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
    }

    // CA-10: 403 Forbidden (Permisos denegados)
    @Test
    fun `CA-10 - 403 acceso denegado por permisos retorna Fallida`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(403, fallida.codigo)
    }

    // CA-11: Respuesta 200 pero con cuerpo completamente vacío
    @Test
    fun `CA-11 - 200 con cuerpo vacio genera excepcion y retorna Fallida`() = runTest {
        // En lugar de enviar un arreglo vacío "[]", se envía nada, lo que rompe el convertidor JSON
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
    }
}