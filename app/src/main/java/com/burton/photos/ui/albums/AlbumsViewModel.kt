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
import kotlinx.coroutines.flow.map
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
    val local: StateFlow<Boolean> = repository.sessionState
        .map { it.isLocal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isLocal)
    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch { repository.refreshAlbums() }
    }

    fun refresh() {
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
    private val id: String = checkNotNull(savedStateHandle["albumId"])
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    val local: StateFlow<Boolean> = repository.sessionState
        .map { it.isLocal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isLocal)

    init {
        load()
    }

    fun reload() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _error.value = null
                _album.value = repository.album(id)
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }

    fun mediaUrl(path: String?) = repository.mediaUrl(path)
}
