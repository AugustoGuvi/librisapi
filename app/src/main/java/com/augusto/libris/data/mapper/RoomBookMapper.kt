package com.augusto.libris.data.mapper

import com.augusto.libris.data.local.BookCacheEntity
import com.augusto.libris.data.local.FavoriteEntity
import com.augusto.libris.domain.model.Book
import com.google.gson.Gson

/** Los modelos de dominio nunca dependen directamente de las clases Room. */
private val gson = Gson()

fun Book.toCacheEntity(queryKey: String, position: Int): BookCacheEntity =
    BookCacheEntity(queryKey, id, position, gson.toJson(this))

fun BookCacheEntity.toBook(): Book = gson.fromJson(bookJson, Book::class.java)

fun Book.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
    bookId = id,
    title = title,
    authorLabel = authors.joinToString(", "),
    coverUrl = coverUrl,
    bookJson = gson.toJson(this)
)

fun FavoriteEntity.toBook(): Book = gson.fromJson(bookJson, Book::class.java)
