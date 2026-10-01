package com.burton.photos.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.LibrarySnapshot
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.UploadJob
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    val library: StateFlow<LibrarySnapshot> = repository.library.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibrarySnapshot(),
    )
    val uploads: StateFlow<List<UploadJob>> = repository.uploads.jobs

    fun mediaUrl(path: String?) = repository.mediaUrl(path)

    fun load(query: PhotoQuery) {
        viewModelScope.launch { repository.refreshLibrary(query) }
    }

    fun loadMore() {
        viewModelScope.launch { repository.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshLibrary(library.value.query) }
    }
}
