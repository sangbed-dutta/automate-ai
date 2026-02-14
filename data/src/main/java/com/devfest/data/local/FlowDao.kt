package com.devfest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FlowEntity)

    @Query("SELECT * FROM flows WHERE id = :id")
    suspend fun getById(id: String): FlowEntity?

    @Query("SELECT * FROM flows ORDER BY title")
    fun observeAll(): Flow<List<FlowEntity>>
}
