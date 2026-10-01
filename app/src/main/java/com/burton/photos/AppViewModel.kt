package com.burton.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    val session: StateFlow<SessionState> = repository.sessionState

    init {
        viewModelScope.launch { repository.start() }
    }
}
