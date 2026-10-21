package com.augusto.libris.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augusto.libris.domain.model.Book
import com.augusto.libris.ui.components.BookResultCard
import com.augusto.libris.ui.components.LoadingPanel
import com.augusto.libris.ui.components.MessagePanel
import com.augusto.libris.ui.theme.*
import com.augusto.libris.ui.viewmodel.FavoritesUiState
import com.augusto.libris.ui.viewmodel.SavedFavorite

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit,
    onRemoveFavorite: (Book) -> Unit,
    onToggleSort: () -> Unit,
    onSaveNote: (String, String) -> Unit
) {
    var editing by remember { mutableStateOf<SavedFavorite?>(null) }
    var noteInput by remember(editing?.book?.id) { mutableStateOf(editing?.note.orEmpty()) }

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Nota de lectura") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(editing?.book?.title.orEmpty())
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Mi nota") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editing?.let { onSaveNote(it.book.id, noteInput) }
                    editing = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancelar") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Paper).safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver a buscar")
                }
                Text("Mis favoritos", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Rust)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.books.size} libros guardados", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = SecondaryInk)
                OutlinedButton(onClick = onToggleSort, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.sortByTitle) "Por nombre" else "Más recientes")
                }
            }
        }
        when {
            state.loading -> item { LoadingPanel("Leyendo favoritos guardados...") }
            state.books.isEmpty() -> item {
                MessagePanel("Aún no tienes favoritos. Pulsa el corazón de cualquier libro para guardarlo. Funcionan sin internet.")
            }
            else -> items(state.books, key = { it.book.id }) { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    BookResultCard(
                        book = entry.book,
                        isFavorite = true,
                        onClick = { onBookClick(entry.book) },
                        onFavoriteClick = { onRemoveFavorite(entry.book) }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.note?.takeIf { it.isNotBlank() } ?: "Sin nota personal",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall, color = SecondaryInk,
                            maxLines = 2)
                        TextButton(onClick = { editing = entry }) {
                            Icon(Icons.Default.EditNote, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Editar nota")
                        }
                    }
                }
            }
        }
        item { Text("Estos libros y tus notas se conservan al cerrar la app o activar el modo avión.",
            color = SecondaryInk, style = MaterialTheme.typography.labelSmall) }
    }
}
