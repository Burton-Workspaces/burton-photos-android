package com.burton.photos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.Health
import com.burton.photos.domain.SessionState
import com.burton.photos.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        viewModelScope.launch {
            repository.sessionState.collect { state ->
                if (state is SessionState.SignedIn) {
                    _user.value = state.user
                    _mode.value = state.mode
                }
            }
        }
        viewModelScope.launch {
            _health.value = runCatching { repository.health() }.getOrNull()
        }
    }

    fun origin(): String = repository.mediaUrl("/")?.trimEnd('/') ?: ""

    fun signOut() {
        viewModelScope.launch { repository.logout() }
    }
}
