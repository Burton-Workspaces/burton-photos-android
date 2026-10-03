package com.burton.photos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.Health
import com.burton.photos.domain.SessionState
import com.burton.photos.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    private val _mode = MutableStateFlow("")
    val mode: StateFlow<String> = _mode
    private val _health = MutableStateFlow<Health?>(null)
    val health: StateFlow<Health?> = _health
    val local: StateFlow<Boolean> = repository.sessionState
        .map { it.isLocal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isLocal)

    init {
        viewModelScope.launch {
            repository.sessionState.collect { state ->
                if (state is SessionState.SignedIn) {
                    _user.value = state.user
                    _mode.value = state.mode
                    if (_health.value == null) {
                        _health.value = runCatching { repository.health() }.getOrNull()
                    }
                } else {
                    _user.value = null
                    _mode.value = ""
                    _health.value = null
                }
            }
        }
    }

    fun origin(): String = repository.mediaUrl("/")?.trimEnd('/') ?: ""

    fun signOut() {
        viewModelScope.launch { repository.logout() }
    }
}
