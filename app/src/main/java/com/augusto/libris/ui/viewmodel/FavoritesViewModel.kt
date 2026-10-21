package com.augusto.libris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augusto.libris.data.mapper.toBook
import com.augusto.libris.data.repository.FavoritesRepository
import com.augusto.libris.domain.model.Book
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedFavorite(val book: Book, val note: String?, val addedAt: Long)

data class FavoritesUiState(
    val books: List<SavedFavorite> = emptyList(),
    val sortByTitle: Boolean = false,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel(private val repository: FavoritesRepository) : ViewModel() {
    private val sortByTitle = MutableStateFlow(false)

    val uiState: StateFlow<FavoritesUiState> = sortByTitle.flatMapLatest { alphabetical ->
        (if (alphabetical) repository.observeAlphabetical() else repository.observeRecent())
            .map { entries ->
                FavoritesUiState(
                    books = entries.map { SavedFavorite(it.toBook(), it.note, it.addedAt) },
                    sortByTitle = alphabetical,
                    loading = false
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun toggleSort() { sortByTitle.value = !sortByTitle.value }

    fun toggleFavorite(book: Book) { viewModelScope.launch { repository.toggle(book) } }

    fun removeFavorite(book: Book) { viewModelScope.launch { repository.remove(book.id) } }

    fun saveNote(bookId: String, note: String) {
        viewModelScope.launch { repository.saveNote(bookId, note) }
    }
}
