package com.augusto.libris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookCacheDao {
    @Query("SELECT * FROM book_cache WHERE queryKey = :queryKey ORDER BY position ASC")
    fun observeBooks(queryKey: String): Flow<List<BookCacheEntity>>

    @Query("SELECT * FROM search_cache WHERE queryKey = :queryKey LIMIT 1")
    fun observeSearch(queryKey: String): Flow<SearchCacheEntity?>

    @Query("SELECT * FROM search_cache WHERE queryKey = :queryKey LIMIT 1")
    suspend fun getSearch(queryKey: String): SearchCacheEntity?

    @Query("SELECT COUNT(*) FROM book_cache WHERE queryKey = :queryKey")
    suspend fun countBooks(queryKey: String): Int

    @Query("SELECT bookId FROM book_cache WHERE queryKey = :queryKey")
    suspend fun getBookIds(queryKey: String): List<String>

    @Query("DELETE FROM book_cache WHERE queryKey = :queryKey")
    suspend fun deleteBooks(queryKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSearch(search: SearchCacheEntity)

    /** La API se consulta ANTES: fallo de red nunca destruye la cache anterior. */
    @Transaction
    suspend fun replaceResults(search: SearchCacheEntity, books: List<BookCacheEntity>) {
        deleteBooks(search.queryKey)
        insertBooks(books)
        saveSearch(search)
    }

    /** No borra paginas anteriores: anade los resultados nuevos de forma atomica. */
    @Transaction
    suspend fun appendPage(search: SearchCacheEntity, books: List<BookCacheEntity>) {
        insertBooks(books)
        saveSearch(search)
    }
}
