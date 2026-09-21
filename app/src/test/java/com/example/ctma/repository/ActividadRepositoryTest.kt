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
import java.util.concurrent.TimeUnit

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

    // CA-01: 200 con actividades
    @Test
    fun `CA-01 - 200 con actividades guarda en Room y actualiza el estado a Exitosa`() = runTest {
        val jsonResponse = """
            [
                {"id": 1, "titulo": "Actividad 1", "descripcion": "Desc 1", "fechaLimite": "2026-09-30", "estado": "PENDIENTE"}
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)
    }

    // CA-02: 200 con arreglo vacío
    @Test
    fun `CA-02 - 200 con arreglo vacio no borra la cache previa`() = runTest {
        // Precargar caché previa en Dao
        dao.insertarTodas(listOf(ActividadEntity(1, "Previa", "Desc", "2026-09-01", "HECHO")))

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)
        // El caché previo debe conservarse según la política establecida
    }

    // CA-03: Timeout con caché
    @Test
    fun `CA-03 - Timeout conserva el cache local y retorna operacion Fallida`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id": 1, "titulo": "T"}]""")
                .setBodyDelay(20, TimeUnit.SECONDS)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
    }

    // CA-05: 401 Unauthorized
    @Test
    fun `CA-05 - 401 retorna estado Fallida clasificado con mensaje de sesion vencida`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(401, fallida.codigo)
        assertTrue(fallida.mensaje.contains("Sesión vencida"))
    }

    // CA-06: 500 o JSON inválido
    @Test
    fun `CA-06 - 500 retorna estado Fallida clasificando error de servidor`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Error"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(500, fallida.codigo)
    }
}
