package com.burton.photos.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.local.LocalIds
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotosRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["id"])
    private val _photo = MutableStateFlow<Photo?>(null)
    val photo: StateFlow<Photo?> = _photo
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    val local: StateFlow<Boolean> = repository.sessionState
        .map { it.isLocal || LocalIds.isPhoto(id) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repository.isLocal || LocalIds.isPhoto(id),
        )

    init {
        viewModelScope.launch {
            try {
                _photo.value = repository.photo(id)
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }

    fun mediaUrl(path: String?) = repository.mediaUrl(path)

    fun toggleFavorite() {
        val current = _photo.value ?: return
        viewModelScope.launch {
            _photo.value = repository.toggleFavorite(current)
        }
    }

    fun toggleArchive() {
        val current = _photo.value ?: return
        viewModelScope.launch {
            _photo.value = repository.toggleArchive(current)
        }
    }
}
