package com.augusto.libris.data.mapper

import com.augusto.libris.data.dto.OpenLibraryBookDto
import com.augusto.libris.domain.model.Book

/** Una obra de Open Library se transforma al mismo modelo Book de la app. */
fun OpenLibraryBookDto.toBook(): Book? {
    val titleValue = title?.takeIf { it.isNotBlank() } ?: return null
    val keyValue = key?.takeIf { Regex("^/works/OL[0-9]+W$").matches(it) } ?: return null
    return Book(
        id = "openlibrary:$keyValue",
        title = titleValue,
        subtitle = null,
        authors = authorNames.orEmpty().filter { it.isNotBlank() },
        descriptionHtml = null,
        coverUrl = coverId?.takeIf { it > 0 }?.let {
            "https://covers.openlibrary.org/b/id/$it-M.jpg"
        },
        publisher = publisher?.firstOrNull { it.isNotBlank() },
        publishedDate = firstPublishYear?.toString(),
        pageCount = pageCount,
        language = language?.firstOrNull(),
        categories = emptyList(),
        isbn = isbn?.firstOrNull { it.isNotBlank() },
        infoUrl = "https://openlibrary.org$keyValue"
    )
}
