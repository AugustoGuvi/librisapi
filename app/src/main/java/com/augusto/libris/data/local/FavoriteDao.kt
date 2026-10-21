package com.augusto.libris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeRecent(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY title COLLATE NOCASE ASC")
    fun observeByTitle(): Flow<List<FavoriteEntity>>

    @Query("SELECT bookId FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE bookId = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE bookId = :id")
    suspend fun delete(id: String)

    @Query("UPDATE favorites SET note = :note WHERE bookId = :id")
    suspend fun updateNote(id: String, note: String?)
}
