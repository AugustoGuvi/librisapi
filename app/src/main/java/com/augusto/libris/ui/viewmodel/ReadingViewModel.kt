package com.augusto.libris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augusto.libris.data.repository.GutenbergRepository
import com.augusto.libris.domain.model.Book
import com.augusto.libris.domain.model.FreeEdition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingUiState(
    val loading: Boolean = false,
    val edition: FreeEdition? = null,
    val message: String? = null
)

class ReadingViewModel(private val repository: GutenbergRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()
    private var request: Job? = null

    fun load(book: Book) {
        request?.cancel()
        request = viewModelScope.launch {
            _uiState.value = ReadingUiState(loading = true)
            try {
                val edition = repository.findEdition(book)
                _uiState.value = if (edition == null) {
                    ReadingUiState(message = "No se encontró una coincidencia confiable en Project Gutenberg para este libro.")
                } else {
                    ReadingUiState(edition = edition)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = ReadingUiState(
                    message = "No fue posible consultar Gutendex. Comprueba tu conexión."
                )
            }
        }
    }
}
