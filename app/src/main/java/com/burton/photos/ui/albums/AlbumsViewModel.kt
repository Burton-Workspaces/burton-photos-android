package com.burton.photos.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    val albums: StateFlow<List<Album>> = repository.albums.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch { repository.refreshAlbums() }
    }

    fun mediaUrl(path: String?) = repository.mediaUrl(path)

    fun create(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _creating.value = true
            try {
                repository.createAlbum(title.trim())
                _error.value = null
            } catch (error: Exception) {
                _error.value = error.message
            } finally {
                _creating.value = false
            }
        }
    }
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotosRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["id"])
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            try {
                _album.value = repository.album(id)
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }

    fun mediaUrl(path: String?) = repository.mediaUrl(path)
}
