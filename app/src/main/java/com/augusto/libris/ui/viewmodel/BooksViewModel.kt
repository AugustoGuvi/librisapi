package com.augusto.libris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augusto.libris.data.repository.BookRepository
import com.augusto.libris.data.repository.CatalogSource
import com.augusto.libris.data.repository.FavoritesRepository
import com.augusto.libris.domain.model.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BooksUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val loadMoreError: String? = null,
    val books: List<Book> = emptyList(),
    val totalItems: Int = 0,
    val currentQuery: String = "Don Quijote",
    val error: String? = null,
    val hasSavedCopy: Boolean = false,
    val savedAt: Long? = null,
    val favoriteIds: Set<String> = emptySet(),
    val source: CatalogSource? = null,
    val notice: String? = null
)

private data class SearchNetworkState(
    val query: String = "Don Quijote",
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val loadMoreError: String? = null,
    val error: String? = null,
    val notice: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BooksViewModel(
    private val repository: BookRepository,
    private val favorites: FavoritesRepository
) : ViewModel() {
    private val selectedQuery = MutableStateFlow("Don Quijote")
    private val network = MutableStateFlow(SearchNetworkState())
    private var request: Job? = null
    private var pagination: Job? = null

    // Fuente unica de verdad: los libros de la pantalla SIEMPRE provienen de Room.
    val uiState: StateFlow<BooksUiState> = combine(
        selectedQuery.flatMapLatest(repository::observeSaved),
        network,
        favorites.observeIds()
    ) { saved, net, ids ->
        BooksUiState(
            isLoading = net.query == saved.query && net.loading,
            isLoadingMore = net.query == saved.query && net.loadingMore,
            loadMoreError = if (net.query == saved.query) net.loadMoreError else null,
            hasMore = saved.hasSavedCopy && saved.books.size < saved.totalItems &&
                !saved.endReached && !(net.query == saved.query && net.endReached),
            books = saved.books,
            totalItems = saved.totalItems,
            currentQuery = saved.query,
            error = if (net.query == saved.query) net.error else null,
            hasSavedCopy = saved.hasSavedCopy,
            savedAt = saved.savedAt,
            favoriteIds = ids,
            source = saved.source,
            notice = if (net.query == saved.query) net.notice else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BooksUiState())

    init {
        viewModelScope.launch {
            // Tal como pide el manual, un inicio con cache NO vuelve a consultar la API.
            val saved = try { repository.hasSaved("Don Quijote") } catch (_: Exception) { false }
            if (selectedQuery.value == "Don Quijote") {
                if (saved) network.value = SearchNetworkState("Don Quijote", loading = false)
                else search("Don Quijote")
            }
        }
    }

    fun search(rawQuery: String) {
        val query = rawQuery.trim().replace(Regex("\\s+"), " ")
        if (query.isEmpty()) {
            network.value = SearchNetworkState(selectedQuery.value, loading = false,
                error = "Escribe un titulo o un autor para buscar.")
            return
        }
        request?.cancel()
        pagination?.cancel()
        selectedQuery.value = query
        network.value = SearchNetworkState(query, loading = true)
        request = viewModelScope.launch {
            try {
                val result = repository.refresh(query)
                network.value = SearchNetworkState(query, loading = false, notice = result.notice)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                network.value = SearchNetworkState(
                    query, loading = false,
                    error = error.message ?: "No se pudo conectar con los catálogos. Si ya buscaste este libro, verás su copia guardada."
                )
            }
        }
    }

    fun refresh() = search(selectedQuery.value)

    fun loadMore() {
        val current = uiState.value
        val net = network.value
        if (!current.hasMore || net.loading || net.loadingMore) return
        val query = selectedQuery.value
        network.value = net.copy(loadingMore = true, loadMoreError = null)
        pagination = viewModelScope.launch {
            try {
                val result = repository.loadMore(query)
                if (selectedQuery.value == query) {
                    network.value = network.value.copy(
                        loadingMore = false,
                        endReached = result.endReached
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (selectedQuery.value == query) {
                    network.value = network.value.copy(
                        loadingMore = false,
                        loadMoreError = "Sin conexion para cargar otra pagina. Los libros guardados siguen disponibles."
                    )
                }
            }
        }
    }
}
