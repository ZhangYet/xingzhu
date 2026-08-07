package com.xingzhu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoemDao {

    @Query("SELECT * FROM poem ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<PoemEntity>>

    @Query("SELECT COUNT(*) FROM poem")
    suspend fun count(): Long

    @Query("SELECT * FROM poem WHERE id = :id")
    fun observeById(id: Long): Flow<PoemEntity?>

    @Query("SELECT * FROM poem WHERE title = :title AND author = :author LIMIT 1")
    suspend fun findByTitleAuthor(title: String, author: String): PoemEntity?

    @Query("SELECT * FROM poem WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun search(query: String): Flow<List<PoemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poem: PoemEntity): Long

    @Query("UPDATE poem SET annotationJson = :json WHERE id = :id")
    suspend fun updateAnnotation(id: Long, json: String?)

    @Query("DELETE FROM poem WHERE id = :id")
    suspend fun delete(id: Long)
}
