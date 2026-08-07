package com.xingzhun.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoemDao {

    @Query("SELECT * FROM poem ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<PoemEntity>>

    @Query("SELECT * FROM poem WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun search(query: String): Flow<List<PoemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poem: PoemEntity): Long

    @Query("DELETE FROM poem WHERE id = :id")
    suspend fun delete(id: Long)
}
