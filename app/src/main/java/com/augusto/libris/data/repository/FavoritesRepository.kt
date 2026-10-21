package com.augusto.libris.data.repository

import com.augusto.libris.data.local.FavoriteDao
import com.augusto.libris.data.local.FavoriteEntity
import com.augusto.libris.data.mapper.toFavoriteEntity
import com.augusto.libris.domain.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val dao: FavoriteDao) {
    fun observeRecent(): Flow<List<FavoriteEntity>> = dao.observeRecent()
    fun observeAlphabetical(): Flow<List<FavoriteEntity>> = dao.observeByTitle()
    fun observeIds(): Flow<Set<String>> = dao.observeFavoriteIds().map { it.toSet() }

    suspend fun toggle(book: Book) {
        if (dao.exists(book.id)) dao.delete(book.id) else dao.insert(book.toFavoriteEntity())
    }

    suspend fun remove(id: String) = dao.delete(id)

    suspend fun saveNote(id: String, note: String) =
        dao.updateNote(id, note.trim().takeIf { it.isNotBlank() })
}
