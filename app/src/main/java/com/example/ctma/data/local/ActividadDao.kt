package com.example.ctma.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActividadDao {

    @Query("SELECT * FROM actividades ORDER BY id ASC")
    fun observarTodas(): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM actividades WHERE id = :id")
    suspend fun obtenerPorId(id: Int): ActividadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(actividades: List<ActividadEntity>)

    @Query("DELETE FROM actividades")
    suspend fun limpiarTodas()
}
