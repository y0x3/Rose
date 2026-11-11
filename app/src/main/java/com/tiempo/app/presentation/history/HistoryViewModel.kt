package com.tiempo.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.model.DayHistory
import com.tiempo.app.domain.usecase.AddJournalImagesUseCase
import com.tiempo.app.domain.usecase.AddJournalNoteUseCase
import com.tiempo.app.domain.usecase.DeleteJournalImageUseCase
import com.tiempo.app.domain.usecase.DeleteJournalNoteUseCase
import com.tiempo.app.domain.usecase.ObserveHistoryUseCase
import com.tiempo.app.domain.usecase.UpdateJournalNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val days: List<DayHistory> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistory: ObserveHistoryUseCase,
    private val addNoteUseCase: AddJournalNoteUseCase,
    private val updateNoteUseCase: UpdateJournalNoteUseCase,
    private val deleteNoteUseCase: DeleteJournalNoteUseCase,
    private val addImagesUseCase: AddJournalImagesUseCase,
    private val deleteImageUseCase: DeleteJournalImageUseCase,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeHistory()
        .map { HistoryUiState(days = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    /** Día abierto en el detalle (null = ninguno). */
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    fun openDay(date: LocalDate) { _selectedDate.value = date }

    fun openToday() { _selectedDate.value = LocalDate.now(clock) }

    fun closeDay() { _selectedDate.value = null }

    /** Una nota puede ir sin texto si trae fotos. */
    fun addNote(date: LocalDate, text: String, photos: List<String> = emptyList()) {
        if (text.isBlank() && photos.isEmpty()) return
        viewModelScope.launch {
            val id = addNoteUseCase(date, text)
            if (photos.isNotEmpty()) addImagesUseCase(id, photos)
        }
    }

    fun updateNote(id: Long, text: String) {
        viewModelScope.launch { updateNoteUseCase(id, text) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { deleteNoteUseCase(id) }
    }

    fun addPhotos(noteId: Long, photos: List<String>) {
        if (photos.isEmpty()) return
        viewModelScope.launch { addImagesUseCase(noteId, photos) }
    }

    fun deleteImage(imageId: Long) {
        viewModelScope.launch { deleteImageUseCase(imageId) }
    }
}