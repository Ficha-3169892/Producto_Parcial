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

    private val flow =
        MutableStateFlow<List<ActividadEntity>>(emptyList())

    override fun observarTodas(): Flow<List<ActividadEntity>> {
        return flow
    }

    override suspend fun obtenerPorId(id: Int): ActividadEntity? {
        return db.find { it.id == id }
    }

    override suspend fun insertarTodas(
        actividades: List<ActividadEntity>
    ) {
        db.removeAll { existente ->
            actividades.any { it.id == existente.id }
        }

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

        val okHttpClient =
            NetworkModule.crearOkHttpClient(tokenProvider)

        api = NetworkModule.crearActividadApi(
            mockWebServer.url("/").toString(),
            okHttpClient
        )

        dao = FakeActividadDao()

        val remoteDataSource =
            RemoteActividadDataSource(api)

        repository =
            ActividadRepositoryImpl(
                remoteDataSource,
                dao
            )
    }


    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }


    // ============================================================
    // HU-01 - Actualización de información
    // CA-01 - Cuando existen actividades disponibles en el servidor,
    // el sistema debe actualizar la información correctamente.
    // ============================================================

    @Test
    fun `HU-01 - CA-01 - actualiza las actividades disponibles`() =
        runTest {

            val respuestaServidor = """
                [
                    {
                        "id": 10,
                        "titulo": "Diseño de interfaz",
                        "descripcion": "Crear una interfaz para la aplicación",
                        "fechaLimite": "2026-10-15",
                        "estado": "PENDIENTE"
                    },
                    {
                        "id": 11,
                        "titulo": "Pruebas del sistema",
                        "descripcion": "Realizar pruebas funcionales",
                        "fechaLimite": "2026-10-20",
                        "estado": "EN_PROCESO"
                    }
                ]
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(respuestaServidor)
            )

            val resultado = repository.refresh()

            assertTrue(
                resultado is OperacionUiState.Exitosa
            )
        }


    // ============================================================
    // HU-02 - Consulta sin resultados
    // CA-02 - Cuando el servidor no tiene actividades para mostrar,
    // la aplicación debe completar la operación correctamente.
    // ============================================================

    @Test
    fun `HU-02 - CA-02 - procesa correctamente una consulta sin resultados`() =
        runTest {

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("[]")
            )

            val resultado = repository.refresh()

            assertTrue(
                resultado is OperacionUiState.Exitosa
            )
        }


    // ============================================================
    // HU-03 - Disponibilidad de la información local
    // CA-03 - Si la conexión con el servidor presenta una demora,
    // el sistema debe informar que la actualización no fue exitosa.
    // ============================================================

    @Test
    fun `HU-03 - CA-03 - controla una demora en la respuesta del servidor`() =
        runTest {

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(
                        """
                        [
                            {
                                "id": 20,
                                "titulo": "Actividad de prueba"
                            }
                        ]
                        """.trimIndent()
                    )
                    .setBodyDelay(
                        20,
                        TimeUnit.SECONDS
                    )
            )

            val resultado = repository.refresh()

            assertTrue(
                resultado is OperacionUiState.Fallida
            )
        }


    // ============================================================
    // HU-04 - Validación de acceso
    // CA-04 - Si el servidor determina que la sesión ya no es válida,
    // el sistema debe identificar correctamente el problema.
    // ============================================================

    @Test
    fun `HU-04 - CA-04 - identifica una sesion no valida`() =
        runTest {

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("Unauthorized")
            )

            val resultado = repository.refresh()

            assertTrue(
                resultado is OperacionUiState.Fallida
            )

            val error =
                resultado as OperacionUiState.Fallida

            assertEquals(
                401,
                error.codigo
            )

            assertTrue(
                error.mensaje.contains("Sesión vencida")
            )
        }


    // ============================================================
    // HU-05 - Disponibilidad del servicio
    // CA-05 - Si el servicio presenta un error interno,
    // la aplicación debe identificar el código recibido.
    // ============================================================

    @Test
    fun `HU-05 - CA-05 - identifica un error interno del servicio`() =
        runTest {

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Service temporarily unavailable")
            )

            val resultado = repository.refresh()

            assertTrue(
                resultado is OperacionUiState.Fallida
            )

            val error =
                resultado as OperacionUiState.Fallida

            assertEquals(
                500,
                error.codigo
            )
        }
}