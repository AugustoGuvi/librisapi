package com.augusto.libris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.augusto.libris.domain.model.Book
import com.augusto.libris.ui.theme.*

@Composable
fun BookCover(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCover),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AutoStories,
            contentDescription = null,
            tint = Honey,
            modifier = Modifier.size(36.dp)
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Portada del libro",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val ColorCover = Ink

@Composable
fun BookResultCard(book: Book, isFavorite: Boolean, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            BookCover(book.coverUrl, Modifier.size(width = 91.dp, height = 128.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Ink
                )
                Text(
                    book.authors.joinToString(", ").ifBlank { "Autor no indicado" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(book.publishedDate, book.publisher).joinToString(" · ")
                        .ifBlank { "Ver ficha" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Rust,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar favorito" else "Guardar favorito",
                        tint = Rust
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Abrir libro", tint = Rust)
            }
        }
    }
}

@Composable
fun MessagePanel(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Paper
    ) {
        Text(
            message,
            color = SecondaryInk,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(17.dp)
        )
    }
}

@Composable
fun LoadingPanel(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Rust, strokeWidth = 2.dp)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = SecondaryInk)
    }
}
