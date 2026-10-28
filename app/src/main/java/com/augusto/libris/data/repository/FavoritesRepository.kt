
package com.augusto.libris.data.repository

import com.augusto.libris.data.local.FavoriteDao
import com.augusto.libris.data.local.FavoriteEntity
import com.augusto.libris.data.mapper.toFavoriteEntity
import com.augusto.libris.data.remote.CloudFavorite
import com.augusto.libris.data.remote.DeleteFavoriteRequest
import com.augusto.libris.data.remote.LibrisCloudApi
import com.augusto.libris.data.remote.UpdateNoteRequest
import com.augusto.libris.domain.model.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// La autenticacion se establece al iniciar sesion.
// Nunca incluir la clave administrativa en el APK.
object CloudSession {
    @Volatile
    var authorization: String? = null
}

class FavoritesRepository(
    private val dao: FavoriteDao,
    private val cloudApi: LibrisCloudApi
) {

    val cloudError = MutableStateFlow<String?>(null)

    fun observeRecent(): Flow<List<FavoriteEntity>> =
        dao.observeRecent()

    fun observeAlphabetical(): Flow<List<FavoriteEntity>> =
        dao.observeByTitle()

    fun observeIds(): Flow<Set<String>> =
        dao.observeFavoriteIds().map { it.toSet() }

    private suspend fun sendToCloud(
        operation: suspend (String) -> Unit
    ) {
        val authorization = CloudSession.authorization ?: return

        try {
            operation(authorization)
            cloudError.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cloudError.value =
                e.message ?: "Error al conectar con SQLite Cloud"
        }
    }

    suspend fun toggle(book: Book) {
        if (dao.exists(book.id)) {
            remove(book.id)
        } else {
            val entity = book.toFavoriteEntity()

            // Guardar en Room
            dao.insert(entity)

            // Guardar en SQLite Cloud si hay sesion
            sendToCloud { authorization ->
                cloudApi.guardarFavorito(
                    authorization,
                    CloudFavorite(
                        bookId = entity.bookId,
                        title = entity.title,
                        authorLabel = entity.authorLabel,
                        coverUrl = entity.coverUrl,
                        bookJson = entity.bookJson,
                        addedAt = entity.addedAt,
                        note = entity.note
                    )
                )
            }
        }
    }

    suspend fun remove(id: String) {
        // Eliminar de Room
        dao.delete(id)

        // Eliminar de SQLite Cloud
        sendToCloud { authorization ->
            cloudApi.eliminarFavorito(
                authorization,
                DeleteFavoriteRequest(bookId = id)
            )
        }
    }

    suspend fun saveNote(id: String, note: String) {
        val newNote = note.trim().takeIf { it.isNotBlank() }

        // Actualizar Room
        dao.updateNote(id, newNote)

        // Actualizar SQLite Cloud
        sendToCloud { authorization ->
            cloudApi.actualizarNota(
                authorization,
                UpdateNoteRequest(
                    bookId = id,
                    note = newNote
                )
            )
        }
    }

    suspend fun importFromCloud() {
        val authorization = CloudSession.authorization ?: return

        try {
            val response = cloudApi.listarFavoritos(authorization)

            response.data.forEach { favorite ->
                dao.insert(
                    FavoriteEntity(
                        bookId = favorite.bookId,
                        title = favorite.title,
                        authorLabel = favorite.authorLabel,
                        coverUrl = favorite.coverUrl,
                        bookJson = favorite.bookJson,
                        addedAt = favorite.addedAt,
                        note = favorite.note
                    )
                )
            }

            cloudError.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cloudError.value =
                e.message ?: "No se pudieron importar favoritos"
        }
    }
}
