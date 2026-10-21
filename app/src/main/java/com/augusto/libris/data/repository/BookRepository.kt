package com.augusto.libris.data.repository

import com.augusto.libris.data.local.BookCacheDao
import com.augusto.libris.data.local.SearchCacheEntity
import com.augusto.libris.data.mapper.toBook
import com.augusto.libris.data.mapper.toCacheEntity
import com.augusto.libris.data.remote.GoogleBooksApi
import com.augusto.libris.data.remote.OpenLibraryApi
import com.augusto.libris.domain.model.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException

enum class CatalogSource { GOOGLE_BOOKS, OPEN_LIBRARY }

data class SearchOutcome(val source: CatalogSource, val notice: String? = null)
data class PageResult(val received: Int, val totalItems: Int, val endReached: Boolean)

/** La red solo escribe en Room; la interfaz observa siempre la base de datos. */
class BookRepository(
    private val googleApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val cacheDao: BookCacheDao,
    private val googleBooksApiKey: String = ""
) {
    data class SavedSearch(
        val query: String,
        val books: List<Book>,
        val totalItems: Int,
        val hasSavedCopy: Boolean,
        val savedAt: Long?,
        val source: CatalogSource?,
        val endReached: Boolean
    )

    fun observeSaved(query: String): Flow<SavedSearch> {
        val key = queryKey(query)
        return combine(cacheDao.observeBooks(key), cacheDao.observeSearch(key)) { books, record ->
            SavedSearch(
                query = query,
                books = books.map { it.toBook() },
                totalItems = record?.totalItems ?: 0,
                hasSavedCopy = record != null,
                savedAt = record?.savedAt,
                source = record?.source?.let { runCatching { CatalogSource.valueOf(it) }.getOrNull() },
                endReached = record != null && record.nextOffset >= record.totalItems
            )
        }
    }

    suspend fun hasSaved(query: String): Boolean = cacheDao.getSearch(queryKey(query)) != null

    private data class FetchedPage(
        val books: List<Book>, val total: Int, val received: Int, val source: CatalogSource
    )

    private suspend fun googlePage(query: String, offset: Int = 0): FetchedPage {
        val response = googleApi.searchBooks(
            query = query, startIndex = offset,
            apiKey = googleBooksApiKey.takeIf { it.isNotBlank() }
        )
        return FetchedPage(
            books = response.items.orEmpty().mapNotNull { it.toBook() }.distinctBy { it.id },
            total = response.totalItems ?: 0,
            received = response.items.orEmpty().size,
            source = CatalogSource.GOOGLE_BOOKS
        )
    }

    private suspend fun openLibraryPage(query: String, offset: Int = 0): FetchedPage {
        val response = openLibraryApi.searchBooks(query = query, offset = offset)
        return FetchedPage(
            books = response.docs.orEmpty().mapNotNull { it.toBook() }.distinctBy { it.id },
            total = response.numFound ?: 0,
            received = response.docs.orEmpty().size,
            source = CatalogSource.OPEN_LIBRARY
        )
    }

    /** Primero Google Books; si falla, Open Library. Nunca borra caché cuando falla la red. */
    suspend fun refresh(query: String): SearchOutcome {
        val key = queryKey(query)
        val (page, outcome) = try {
            googlePage(query) to SearchOutcome(CatalogSource.GOOGLE_BOOKS)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (googleError: Exception) {
            try {
                openLibraryPage(query) to SearchOutcome(
                    CatalogSource.OPEN_LIBRARY,
                    "Google Books no respondió (${describeApiError(googleError)}). " +
                        "Mostrando resultados reales de Open Library."
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (openError: Exception) {
                throw CatalogUnavailableException(
                    "Google Books: ${describeApiError(googleError)}. " +
                        "Open Library: ${describeApiError(openError)}. " +
                        "Verifica la conexión del emulador o teléfono.", openError
                )
            }
        }
        cacheDao.replaceResults(
            SearchCacheEntity(
                queryKey = key, queryText = query, totalItems = page.total,
                source = page.source.name, nextOffset = page.received
            ),
            page.books.mapIndexed { index, book -> book.toCacheEntity(key, index) }
        )
        return outcome
    }

    /** Paginación real: sigue la misma fuente del catálogo y conserva lo ya descargado. */
    suspend fun loadMore(query: String): PageResult {
        val key = queryKey(query)
        val saved = cacheDao.getSearch(key) ?: error("Primero realiza una búsqueda")
        if (saved.nextOffset >= saved.totalItems) {
            return PageResult(0, saved.totalItems, endReached = true)
        }
        val source = CatalogSource.valueOf(saved.source)
        val page = when (source) {
            CatalogSource.GOOGLE_BOOKS -> googlePage(query, saved.nextOffset)
            CatalogSource.OPEN_LIBRARY -> openLibraryPage(query, saved.nextOffset)
        }
        val next = saved.nextOffset + page.received
        val knownIds = cacheDao.getBookIds(key).toSet()
        val newBooks = page.books.filterNot { it.id in knownIds }
        cacheDao.appendPage(
            SearchCacheEntity(
                queryKey = key, queryText = query, totalItems = page.total,
                source = source.name, nextOffset = next
            ),
            newBooks.mapIndexed { index, book -> book.toCacheEntity(key, knownIds.size + index) }
        )
        return PageResult(page.received, page.total, page.received == 0 || next >= page.total)
    }

    companion object {
        fun queryKey(query: String): String =
            query.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    }
}

class CatalogUnavailableException(message: String, cause: Throwable) : IOException(message, cause)

fun describeApiError(error: Throwable): String = when (error) {
    is HttpException -> when (error.code()) {
        401 -> "HTTP 401: acceso no autorizado"
        403 -> "HTTP 403: acceso restringido, puede requerirse clave API"
        429 -> "HTTP 429: límite de solicitudes excedido"
        in 500..599 -> "HTTP ${error.code()}: servicio no disponible"
        else -> "HTTP ${error.code()}"
    }
    is UnknownHostException -> "sin conexión o problema DNS"
    is SocketTimeoutException -> "tiempo de espera agotado"
    is SSLException -> "problema de certificado SSL (revisa fecha y hora)"
    is IOException -> "error de red: ${error.javaClass.simpleName}"
    else -> "respuesta inválida: ${error.javaClass.simpleName}"
}
