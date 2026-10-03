package com.burton.photos.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.CalendarYear
import com.burton.photos.domain.CoverItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BrowseKind { Folders, Labels, People, Moments }

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<CoverItem>>(emptyList())
    val items: StateFlow<List<CoverItem>> = _items
    private val _years = MutableStateFlow<List<CalendarYear>>(emptyList())
    val years: StateFlow<List<CalendarYear>> = _years
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    val local: StateFlow<Boolean> = repository.sessionState
        .map { it.isLocal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isLocal)

    fun mediaUrl(path: String?) = repository.mediaUrl(path)

    fun load(kind: BrowseKind) {
        viewModelScope.launch {
            try {
                _error.value = null
                _items.value = when (kind) {
                    BrowseKind.Folders -> repository.folders()
                    BrowseKind.Labels -> repository.labels()
                    BrowseKind.People -> repository.people()
                    BrowseKind.Moments -> repository.moments()
                }
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }

    fun loadCalendar() {
        viewModelScope.launch {
            try {
                _error.value = null
                _years.value = repository.calendar()
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }
}
