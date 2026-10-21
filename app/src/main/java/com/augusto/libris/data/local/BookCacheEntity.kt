package com.augusto.libris.data.local

import androidx.room.Entity
import androidx.room.Index

/** Clave compuesta: un libro puede aparecer en varias busquedas sin sobrescribirse. */
@Entity(tableName = "book_cache", primaryKeys = ["queryKey", "bookId"],
    indices = [Index(value = ["queryKey"])])
data class BookCacheEntity(
    val queryKey: String,
    val bookId: String,
    val position: Int,
    val bookJson: String
)
