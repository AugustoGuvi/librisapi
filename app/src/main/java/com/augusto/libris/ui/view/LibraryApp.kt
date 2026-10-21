package com.augusto.libris.ui.view

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augusto.libris.LibraryApplication
import com.augusto.libris.di.LibraryViewModelFactory
import com.augusto.libris.domain.model.Book
import com.augusto.libris.ui.viewmodel.AuthorViewModel
import com.augusto.libris.ui.viewmodel.BooksViewModel
import com.augusto.libris.ui.viewmodel.FavoritesViewModel
import com.augusto.libris.ui.viewmodel.ReadingViewModel

/** Mantiene las tres APIs y agrega navegacion a favoritos Room. */
@Composable
fun LibraryApp() {
    val app = LocalContext.current.applicationContext as LibraryApplication
    val factory = remember(app) { LibraryViewModelFactory(app.container) }
    val booksVm: BooksViewModel = viewModel(factory = factory)
    val authorVm: AuthorViewModel = viewModel(factory = factory)
    val readingVm: ReadingViewModel = viewModel(factory = factory)
    val favoritesVm: FavoritesViewModel = viewModel(factory = factory)

    val booksState by booksVm.uiState.collectAsState()
    val authorState by authorVm.uiState.collectAsState()
    val readingState by readingVm.uiState.collectAsState()
    val favoritesState by favoritesVm.uiState.collectAsState()
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showFavorites by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedBook != null || showFavorites) {
        if (selectedBook != null) selectedBook = null else showFavorites = false
    }
    LaunchedEffect(selectedBook?.id) {
        selectedBook?.let { book ->
            authorVm.load(book.authors.firstOrNull())
            readingVm.load(book)
        }
    }

    val activeBook = selectedBook
    when {
        activeBook != null -> DetailScreen(
            book = activeBook,
            authorState = authorState,
            readingState = readingState,
            isFavorite = activeBook.id in booksState.favoriteIds ||
                favoritesState.books.any { it.book.id == activeBook.id },
            onToggleFavorite = { favoritesVm.toggleFavorite(activeBook) },
            onBack = { selectedBook = null },
            onWorkClick = { title ->
                selectedBook = null
                showFavorites = false
                booksVm.search(title)
            }
        )
        showFavorites -> FavoritesScreen(
            state = favoritesState,
            onBack = { showFavorites = false },
            onBookClick = { selectedBook = it },
            onRemoveFavorite = favoritesVm::removeFavorite,
            onToggleSort = favoritesVm::toggleSort,
            onSaveNote = favoritesVm::saveNote
        )
        else -> SearchScreen(
            state = booksState,
            onSearch = booksVm::search,
            onRefresh = booksVm::refresh,
            onLoadMore = booksVm::loadMore,
            onBookClick = { selectedBook = it },
            onFavoriteClick = favoritesVm::toggleFavorite,
            onViewFavorites = { showFavorites = true }
        )
    }
}
