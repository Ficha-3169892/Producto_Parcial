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
import okhttp3.mockwebserver.SocketPolicy
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

    // CA-04: Timeout sin caché
    @Test
    fun `CA-04 - Timeout sin cache retorna Fallida y Room permanece vacio`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id": 1, "titulo": "T"}]""")
                .setBodyDelay(1, TimeUnit.SECONDS)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val deGuardadas = dao.observarTodas().first()
        assertTrue(deGuardadas.isEmpty())
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
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida

        assertEquals(404, fallida.codigo)
    }

    // CA-11: Solicitud incorrecta
    @Test
    fun `CA-11 - Error 400 retorna Fallida con codigo de solicitud incorrecta`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida

        assertEquals(400, fallida.codigo)
    }

    // CA-12: Servicio no disponible con caché
    @Test
    fun `CA-12 - Error 503 conserva el cache local y retorna Fallida`() = runTest {
        val actividadPrevia = ActividadEntity(
            id = 5,
            titulo = "Actividad en cache",
            descripcion = "Desc",
            fechaLimite = "2026-09-25",
            estado = "PENDIENTE"
        )
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Unavailable")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(503, fallida.codigo)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Actividad en cache", deGuardadas[0].titulo)
    }

    // CA-13: JSON inválido
    @Test
    fun `CA-13 - 200 con JSON invalido retorna Fallida y no modifica el cache`() = runTest {
        val actividadPrevia = ActividadEntity(
            id = 7,
            titulo = "Cache intacto",
            descripcion = "Desc",
            fechaLimite = "2026-09-28",
            estado = "HECHO"
        )
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{esto no es un json valido")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Cache intacto", deGuardadas[0].titulo)
    }

    // CA-14: Recuperación después de un error
    @Test
    fun `CA-14 - Un refresh exitoso despues de un error guarda las actividades`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Error"))

        val jsonResponse = """
            [
                {
                    "id": 40,
                    "titulo": "Actividad recuperada",
                    "descripcion": "Desc recuperada",
                    "fechaLimite": "2026-11-05",
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

        val primerResultado = repository.refresh()
        assertTrue(primerResultado is OperacionUiState.Fallida)
        assertTrue(dao.observarTodas().first().isEmpty())

        val segundoResultado = repository.refresh()
        assertTrue(segundoResultado is OperacionUiState.Exitosa)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals(40, deGuardadas[0].id)
        assertEquals("Actividad recuperada", deGuardadas[0].titulo)
    }

    // CA-15: Conexión interrumpida con caché
    @Test
    fun `CA-15 - Conexion interrumpida conserva el cache local y retorna Fallida`() = runTest {
        val actividadPrevia = ActividadEntity(
            id = 9,
            titulo = "Cache sin conexion",
            descripcion = "Desc",
            fechaLimite = "2026-09-29",
            estado = "PENDIENTE"
        )
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Cache sin conexion", deGuardadas[0].titulo)
    }
    // CA-16: Rate Limit / Límite de peticiones (429)
    @Test
    fun `CA-16 - Error 429 Too Many Requests retorna Fallida con codigo 429`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("Too Many Requests")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(429, fallida.codigo)
    }

    // CA-17: Respuesta 200 pero con cuerpo totalmente vacío (Blank/Empty Body)
    @Test
    fun `CA-17 - 200 con cuerpo vacio falla en el parseo y no altera el cache`() = runTest {
        val actividadPrevia = ActividadEntity(12, "Previa", "Desc", "2026-09-20", "PENDIENTE")
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("") // Body vacío (provoca excepción de parseo)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Previa", deGuardadas[0].titulo)
    }

    // CA-18: Interrupción de conexión durante la transmisión del cuerpo
    @Test
    fun `CA-18 - Corte de conexion a mitad del response body retorna Fallida y conserva el cache`() = runTest {
        val actividadPrevia = ActividadEntity(15, "Cache Seguro", "Desc", "2026-09-21", "HECHO")
        dao.insertarTodas(listOf(actividadPrevia))

        val partialJson = """[{"id": 99, "titulo": "Incompleto", "descripcion":"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(partialJson)
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Cache Seguro", deGuardadas[0].titulo)
    }

    // CA-19: Manejo de IDs duplicados en la respuesta del servidor
    @Test
    fun `CA-19 - Servidor retorna elementos con ID duplicado y el DAO sobrescribe correctamente`() = runTest {
        val jsonResponse = """
            [
                {"id": 5, "titulo": "Version Inicial", "descripcion": "V1", "fechaLimite": "2026-10-01", "estado": "PENDIENTE"},
                {"id": 5, "titulo": "Version Corregida", "descripcion": "V2", "fechaLimite": "2026-10-01", "estado": "HECHO"}
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

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Version Corregida", deGuardadas[0].titulo)
        assertEquals("HECHO", deGuardadas[0].estado)
    }

    // CA-20: Error de Gateway / Servidor de entrada (502 Bad Gateway)
    @Test
    fun `CA-20 - Error 502 Bad Gateway retorna Fallida clasificando error de puerta de enlace`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setBody("Bad Gateway")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(502, fallida.codigo)
        assertTrue(dao.observarTodas().first().isEmpty())
    }
    // CA-21: Caracteres especiales y codificación UTF-8 (Tildes, 'ñ', emojis)
    @Test
    fun `CA-21 - 200 con caracteres especiales y UTF-8 guarda la informacion sin corromper`() = runTest {
        val jsonResponse = """
            [
                {
                    "id": 100,
                    "titulo": "Evaluación de Software & Programación",
                    "descripcion": "Revisar código con caracteres: ñ, á, é, í, ó, ú y emojis 🚀",
                    "fechaLimite": "2026-12-31",
                    "estado": "PENDIENTE"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(jsonResponse)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Evaluación de Software & Programación", deGuardadas[0].titulo)
        assertTrue(deGuardadas[0].descripcion.contains("🚀"))
    }

    // CA-22: Tolerancia a campos desconocidos/extra en el JSON
    @Test
    fun `CA-22 - 200 con campos no mapeados en el JSON se ignora de forma segura y guarda el objeto`() = runTest {
        val jsonResponse = """
            [
                {
                    "id": 50,
                    "titulo": "Tarea con metadatos extra",
                    "descripcion": "Desc",
                    "fechaLimite": "2026-11-01",
                    "estado": "PENDIENTE",
                    "created_at": "2026-09-20T10:00:00Z",
                    "prioridad_num": 99,
                    "version_schema": 2
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

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals(50, deGuardadas[0].id)
    }

    // CA-23: Violación de contrato de estructura JSON (Objeto único en lugar de Lista)
    @Test
    fun `CA-23 - Respuesta JSON con estructura de objeto unico en lugar de lista retorna Fallida`() = runTest {
        val jsonResponse = """
            {"id": 1, "titulo": "No es un arreglo", "descripcion": "Error de contrato en la API"}
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)
    }

    // CA-24: Procesamiento de carga masiva / Volumen alto de datos
    @Test
    fun `CA-24 - Respuesta con un volumen grande de registros guarda la totalidad en el DAO`() = runTest {
        val listaGrande = (1..100).joinToString(separator = ",") { id ->
            """{"id": $id, "titulo": "Actividad $id", "descripcion": "Desc $id", "fechaLimite": "2026-12-01", "estado": "PENDIENTE"}"""
        }
        val jsonResponse = "[$listaGrande]"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Exitosa)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(100, deGuardadas.size)
    }

    // CA-25: Timeout de Gateway en infraestructura (504 Gateway Timeout)
    @Test
    fun `CA-25 - Error 504 Gateway Timeout retorna Fallida y conserva el cache previo`() = runTest {
        val actividadPrevia = ActividadEntity(88, "Cache previo", "Desc", "2026-09-01", "PENDIENTE")
        dao.insertarTodas(listOf(actividadPrevia))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(504)
                .setBody("Gateway Timeout")
        )

        val resultado = repository.refresh()

        assertTrue(resultado is OperacionUiState.Fallida)

        val fallida = resultado as OperacionUiState.Fallida
        assertEquals(504, fallida.codigo)

        val deGuardadas = dao.observarTodas().first()
        assertEquals(1, deGuardadas.size)
        assertEquals("Cache previo", deGuardadas[0].titulo)
    }
}