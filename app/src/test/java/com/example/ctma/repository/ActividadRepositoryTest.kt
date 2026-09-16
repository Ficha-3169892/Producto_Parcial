package com.example.ctma.repository

import com.example.ctma.auth.SessionTokenProvider
import com.example.ctma.data.local.ActividadDao
import com.example.ctma.data.local.ActividadEntity
import com.example.ctma.data.remote.ActividadApi
import com.example.ctma.data.remote.NetworkModule
import com.example.ctma.data.remote.RemoteActividadDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
        val baseOkHttpClient = NetworkModule.crearOkHttpClient(tokenProvider)
        // Usamos un timeout más corto para que la prueba de timeout sea rápida
        val okHttpClient = baseOkHttpClient.newBuilder()
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .build()

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
    fun `CA-01 - 200 con actividades guarda en Room y actualiza el estado a Exitosa`() {
        runTest {
            val jsonResponse = """
                [
                    {"id": 1, "titulo": "Actividad 1", "descripcion": "Desc 1", "fechaLimite": "2026-09-30", "estado": "PENDIENTE"}
                ]
            """.trimIndent()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Exitosa)
            
            // Verificación real de persistencia en Room/Cache
            val deGuardadas = dao.observarTodas().first()
            assertEquals(1, deGuardadas.size)
            assertEquals(1, deGuardadas[0].id)
            assertEquals("Actividad 1", deGuardadas[0].titulo)
        }
    }

    // CA-02: 200 con arreglo vacío
    @Test
    fun `CA-02 - 200 con arreglo vacio no borra la cache previa`() {
        runTest {
            // Precargar caché previa en Dao
            val actividadPrevia = ActividadEntity(1, "Previa", "Desc", "2026-09-01", "HECHO")
            dao.insertarTodas(listOf(actividadPrevia))

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Exitosa)
            
            // El caché previo debe conservarse según la política establecida
            val deGuardadas = dao.observarTodas().first()
            assertEquals(1, deGuardadas.size)
            assertEquals("Previa", deGuardadas[0].titulo)
        }
    }

    // CA-03: Timeout con caché
    @Test
    fun `CA-03 - Timeout conserva el cache local y retorna operacion Fallida`() {
        runTest {
            // Precargar caché previa en Dao
            val actividadPrevia = ActividadEntity(2, "Cache Existente", "Desc", "2026-09-05", "PENDIENTE")
            dao.insertarTodas(listOf(actividadPrevia))

            mockWebServer.enqueue(
                MockResponse()
                    .setBody("""[{"id": 1, "titulo": "T"}]""")
                    .setBodyDelay(1, TimeUnit.SECONDS)
            )

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Fallida)
            
            // La información local almacenada previamente debe conservarse
            val deGuardadas = dao.observarTodas().first()
            assertEquals(1, deGuardadas.size)
            assertEquals("Cache Existente", deGuardadas[0].titulo)
        }
    }

    // CA-05: 401 Unauthorized
    @Test
    fun `CA-05 - 401 retorna estado Fallida clasificado con mensaje de sesion vencida`() {
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Fallida)
            val fallida = resultado as OperacionUiState.Fallida
            assertEquals(401, fallida.codigo)
            assertTrue(fallida.mensaje.contains("Sesión vencida"))
        }
    }

    // CA-06: 500 o JSON inválido
    @Test
    fun `CA-06 - 500 retorna estado Fallida clasificando error de servidor`() {
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Error"))

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Fallida)
            val fallida = resultado as OperacionUiState.Fallida
            assertEquals(500, fallida.codigo)
            assertTrue(fallida.mensaje.contains("Error del servidor"))
        }
    }

    @Test
    fun `CA-06 - JSON invalido retorna estado Fallida indicando error de formato`() {
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{ malformed json }"))

            val resultado = repository.refresh()

            assertTrue(resultado is OperacionUiState.Fallida)
            val fallida = resultado as OperacionUiState.Fallida
            assertNull(fallida.codigo)
            assertTrue(fallida.mensaje.contains("Error de procesado o formato inválido"))
        }
    }
}

