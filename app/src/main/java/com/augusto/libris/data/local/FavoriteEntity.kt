package com.augusto.libris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Copia completa del libro: un favorito debe poder abrirse sin internet. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val authorLabel: String,
    val coverUrl: String?,
    val bookJson: String,
    val addedAt: Long = System.currentTimeMillis(),
    // Incorporado en la version 3 de la base de datos.
    val note: String? = null
)
