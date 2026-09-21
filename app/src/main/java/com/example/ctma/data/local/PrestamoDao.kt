package com.example.ctma.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "equipos_tabla")
data class EquipoRoomEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val categoria: String,
    val estado: String
)

@Entity(tableName = "solicitudes_tabla")
data class SolicitudRoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val equipoId: Int,
    val ambienteDestino: String,
    val proposito: String,
    val duracionHoras: Int,
    val estado: String,
    val evidenciaUri: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val estadoEvidencia: String? = null
)

@Dao
interface PrestamoDao {

    @Query("SELECT * FROM equipos_tabla")
    suspend fun obtenerTodosLosEquipos(): List<EquipoRoomEntity>

    @Query("SELECT * FROM equipos_tabla WHERE id = :id")
    suspend fun obtenerEquipoPorId(id: Int): EquipoRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEquipos(equipos: List<EquipoRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEquipo(equipo: EquipoRoomEntity)

    @Query("SELECT * FROM solicitudes_tabla ORDER BY id DESC")
    suspend fun obtenerTodasLasSolicitudes(): List<SolicitudRoomEntity>

    @Query("SELECT * FROM solicitudes_tabla WHERE id = :id")
    suspend fun obtenerSolicitudPorId(id: Int): SolicitudRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSolicitud(solicitud: SolicitudRoomEntity)
}
