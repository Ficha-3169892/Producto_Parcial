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
    fun `CA-01 - 200 con actividades guarda en Room y actualiza el estado a Exitosa`() = runTest {
        val jsonResponse = """
            [
                {"id": 1, "titulo": "Actividad 1", "descripcion": "Desc 1", "fechaLimite": "2026-09-30", "estado": "PENDIENTE"}
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)
        
        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals(1, deGuardadas[0].id)
        assertEquals("Actividad 1", deGuardadas[0].titulo)
    }

    // CA-02: 200 con arreglo vacío
    @Test
    fun `CA-02 - 200 con arreglo vacio no borra la cache previa`() = runTest {
        val actividadPrevia = ActividadEntity(1, "Previa", "Desc", "2026-09-01", "HECHO")
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)
        
        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Previa", deGuardadas[0].titulo)
    }

    // CA-03: Timeout con caché
    @Test
    fun `CA-03 - Timeout conserva el cache local y retorna operacion Fallida`() = runTest {
        val actividadPrevia = ActividadEntity(2, "Cache Existente", "Desc", "2026-09-05", "PENDIENTE")
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id": 1, "titulo": "T"}]""")
                .setBodyDelay(1, TimeUnit.SECONDS)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        
        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Cache Existente", deGuardadas[0].titulo)
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

    // CA-07: Actualización de actividades existentes
    @Test
    fun `CA-07 - Actualizar actividades reemplaza la informacion anterior`() = runTest {
        val actividadAnterior = ActividadEntity(
            id = 1,
            titulo = "Actividad antigua",
            descripcion = "Descripcion antigua",
            fechaLimite = "2026-09-01",
            estado = "PENDIENTE"
        )

        dao.insertarTodas(listOf(actividadAnterior))

        val jsonResponse = """
            [
                {
                    "id": 1,
                    "titulo": "Actividad actualizada",
                    "descripcion": "Nueva descripcion",
                    "fechaLimite": "2026-10-15",
                    "estado": "HECHO"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)

        val actividadesGuardadas = dao.observarTodas().first()

        assertEquals(1, actividadesGuardadas.size)
        assertEquals("Actividad actualizada", actividadesGuardadas[0].titulo)
        assertEquals("Nueva descripcion", actividadesGuardadas[0].descripcion)
        assertEquals("HECHO", actividadesGuardadas[0].estado)
    }

    // CA-08: Guardar varias actividades
    @Test
    fun `CA-08 - Respuesta con varias actividades guarda todos los registros`() = runTest {
        val jsonResponse = """
            [
                {
                    "id": 10,
                    "titulo": "Actividad A",
                    "descripcion": "Descripcion A",
                    "fechaLimite": "2026-10-01",
                    "estado": "PENDIENTE"
                },
                {
                    "id": 20,
                    "titulo": "Actividad B",
                    "descripcion": "Descripcion B",
                    "fechaLimite": "2026-10-10",
                    "estado": "HECHO"
                },
                {
                    "id": 30,
                    "titulo": "Actividad C",
                    "descripcion": "Descripcion C",
                    "fechaLimite": "2026-10-20",
                    "estado": "PENDIENTE"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)

        val actividadesGuardadas = dao.observarTodas().first()

        assertEquals(3, actividadesGuardadas.size)

        val ids = actividadesGuardadas.map { it.id }.toSet()

        assertEquals(setOf(10, 20, 30), ids)
    }

    // CA-09: Error de permisos
    @Test
    fun `CA-09 - Error 403 retorna Fallida con codigo de permisos`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("Forbidden")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida

        assertEquals(403, fallida.codigo)
    }

    // CA-10: Recurso no encontrado
    @Test
    fun `CA-10 - Error 404 retorna Fallida con codigo de recurso no encontrado`() = runTest {
        mockWebServer.enqueue(
            MockResponse()                .setResponseCode(404)
                .setBody("Not Found")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida

        assertEquals(404, fallida.codigo)
    }
}
