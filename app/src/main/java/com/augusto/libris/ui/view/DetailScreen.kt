package com.augusto.libris.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.augusto.libris.domain.model.Book
import com.augusto.libris.ui.components.BookCover
import com.augusto.libris.ui.components.LoadingPanel
import com.augusto.libris.ui.components.MessagePanel
import com.augusto.libris.ui.theme.*
import com.augusto.libris.ui.viewmodel.AuthorUiState
import com.augusto.libris.ui.viewmodel.ReadingUiState

private fun android.content.Context.openSafeUrl(url: String) {
    val uri = Uri.parse(url)
    if (uri.scheme != "https") return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
    } catch (_: ActivityNotFoundException) {
        // Si no hay navegador, no se interrumpe la experiencia.
    }
}

@Composable
fun DetailScreen(
    book: Book,
    authorState: AuthorUiState,
    readingState: ReadingUiState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onWorkClick: (String) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Paper).safeDrawingPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Ink)
                }
                Spacer(Modifier.width(6.dp))
                Text("Detalles del libro", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onToggleFavorite) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Guardar como favorito",
                        tint = Rust)
                }
            }
        }
        item {
            Surface(color = Ink, shape = RoundedCornerShape(26.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCover(book.coverUrl, modifier = Modifier.size(112.dp, 165.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text("FICHA DEL LIBRO", style = MaterialTheme.typography.labelSmall, color = Honey)
                        Text(
                            book.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            book.authors.joinToString(", ").ifBlank { "Autor sin especificar" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDAE1EA)
                        )
                        book.publishedDate?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = Honey)
                        }
                    }
                }
            }
        }
        item {
            SectionCard(heading = "Acerca de este libro", subtitle = if (book.id.startsWith("openlibrary:")) "OPEN LIBRARY" else "GOOGLE BOOKS") {
                val info = book.descriptionHtml?.let {
                    HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                }.orEmpty()
                Text(
                    info.ifBlank { "El catálogo no proporciona una descripción para esta edición." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
                Spacer(Modifier.height(10.dp))
                DetailMetadata("Editorial", book.publisher)
                DetailMetadata("Fecha de publicación", book.publishedDate)
                DetailMetadata("Páginas", book.pageCount?.toString())
                DetailMetadata("Idioma", book.language?.uppercase())
                DetailMetadata("ISBN", book.isbn)
                if (!book.infoUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = { context.openSafeUrl(book.infoUrl) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rust),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (book.id.startsWith("openlibrary:")) "Ver ficha en Open Library" else "Ver ficha en Google Books")
                    }
                }
            }
        }
        item {
            SectionCard(heading = "Conoce al autor", subtitle = "OPEN LIBRARY") {
                when {
                    authorState.loading -> LoadingPanel("Consultando información del autor…")
                    authorState.author == null -> MessagePanel(
                        authorState.message ?: "No hay datos del autor disponibles."
                    )
                    else -> {
                        val author = authorState.author
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF1E5)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Rust,
                                    modifier = Modifier.padding(12.dp).size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(author.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val dates = listOfNotNull(author.birthDate, author.deathDate).joinToString(" — ")
                                if (dates.isNotBlank()) {
                                    Text(dates, style = MaterialTheme.typography.labelMedium, color = SecondaryInk)
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            author.bio?.take(1200) ?: "Open Library no tiene una biografía disponible para este autor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink
                        )
                        if (authorState.works.isNotEmpty()) {
                            Spacer(Modifier.height(15.dp))
                            Text("OTRAS OBRAS", style = MaterialTheme.typography.labelSmall, color = Rust, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            authorState.works
                                .filterNot { it.title.equals(book.title, ignoreCase = true) }
                                .forEach { work ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onWorkClick(work.title) }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = Rust, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(9.dp))
                                        Text(work.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Buscar esta obra", tint = SecondaryInk, modifier = Modifier.size(16.dp))
                                    }
                                    HorizontalDivider(color = Border)
                                }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(heading = "Leer o descargar", subtitle = "GUTENDEX · PROJECT GUTENBERG") {
                when {
                    readingState.loading -> LoadingPanel("Buscando una versión gratuita verificable…")
                    readingState.edition == null -> MessagePanel(
                        readingState.message ?: "No hay una edición encontrada en Gutenberg."
                    )
                    else -> {
                        val edition = readingState.edition
                        Text(
                            "Versión encontrada: ${edition.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Idiomas: ${edition.languages.joinToString(", ").ifBlank { "sin especificar" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryInk
                        )
                        Spacer(Modifier.height(11.dp))
                        if (edition.links.isEmpty()) {
                            MessagePanel("Hay un registro del libro, pero no se publicaron enlaces directos compatibles.")
                        } else {
                            edition.links.forEach { link ->
                                FilledTonalButton(
                                    onClick = { context.openSafeUrl(link.url) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(13.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFFFF1E5), contentColor = Rust
                                    )
                                ) {
                                    Text(link.label, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        }
                        OutlinedButton(
                            onClick = { context.openSafeUrl(edition.catalogUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Border),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ver en Project Gutenberg")
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Gutendex identifica obras de dominio público en EE. UU.; verifica la legislación aplicable en tu país. Puede tratarse de otra edición o traducción.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryInk
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Fuentes: Google Books, Open Library y Gutendex. Los catálogos pueden tener información incompleta o diferencias entre ediciones.",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryInk
            )
        }
    }
}

@Composable
private fun SectionCard(
    heading: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Rust, fontWeight = FontWeight.Bold)
            Text(heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            content()
        }
    }
}

@Composable
private fun DetailMetadata(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = SecondaryInk)
        Text(
            value,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodySmall,
            color = Ink,
            fontWeight = FontWeight.SemiBold
        )
    }
}
