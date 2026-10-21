package com.augusto.libris.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Indica la fuente del catálogo y el índice de paginación REAL de la API. */
@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey val queryKey: String,
    val queryText: String,
    val totalItems: Int,
    val savedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'GOOGLE_BOOKS'")
    val source: String = "GOOGLE_BOOKS",
    @ColumnInfo(defaultValue = "0")
    val nextOffset: Int = 0
)
