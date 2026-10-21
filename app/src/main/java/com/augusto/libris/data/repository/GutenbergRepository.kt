package com.augusto.libris.data.repository

import android.util.Log
import com.augusto.libris.data.dto.GutendexBookDto
import com.augusto.libris.data.mapper.toFreeEdition
import com.augusto.libris.data.remote.GutendexApi
import com.augusto.libris.domain.matching.BookMatcher
import com.augusto.libris.domain.model.Book
import com.augusto.libris.domain.model.FreeEdition
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/** Usa el título y comprueba el autor para no confundir ediciones gratuitas. */
class GutenbergRepository(private val api: GutendexApi) {
    suspend fun findEdition(book: Book): FreeEdition? {
        val mainTitle = book.title.substringBefore(':').substringBefore('—').trim()
        val shortTitle = BookMatcher.tokens(mainTitle).take(3).joinToString(" ")
        val queries = listOf(mainTitle, shortTitle).map { it.trim() }
            .filter { it.isNotBlank() }.distinct()

        var anySuccess = false
        var lastFailure: Exception? = null
        for (query in queries) {
            val candidates: List<GutendexBookDto>
            try {
                candidates = api.searchBooks(query).results.orEmpty()
                anySuccess = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastFailure = error
                Log.e("Libris/Gutendex", "Error consultando Gutendex", error)
                if (error is HttpException && error.code() in listOf(401, 403, 429)) break
                continue
            }
            val best = candidates.mapNotNull { dto ->
                if (dto.copyright != false) return@mapNotNull null
                val score = BookMatcher.matchScore(
                    selectedTitle = book.title,
                    selectedAuthors = book.authors,
                    candidateTitle = dto.title.orEmpty(),
                    candidateAuthors = dto.authors.orEmpty().mapNotNull { it.name }
                ) ?: return@mapNotNull null
                dto to score
            }.maxByOrNull { it.second }?.first
            if (best != null) return best.toFreeEdition()
        }
        if (!anySuccess && lastFailure != null) throw lastFailure
        return null
    }
}
