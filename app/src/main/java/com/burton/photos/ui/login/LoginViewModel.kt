package com.burton.photos.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.AuthConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUi(
    val baseUrl: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val config: AuthConfig? = null,
    val probing: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: PhotosRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUi())
    val ui: StateFlow<LoginUi> = _ui

    init {
        viewModelScope.launch {
            val origin = repository.savedOrigin()
            if (origin.isNotBlank()) {
                _ui.value = _ui.value.copy(baseUrl = origin)
            }
        }
    }

    fun setBaseUrl(value: String) {
        _ui.value = _ui.value.copy(baseUrl = value, error = null)
    }

    fun setEmail(value: String) {
        _ui.value = _ui.value.copy(email = value, error = null)
    }

    fun setPassword(value: String) {
        _ui.value = _ui.value.copy(password = value, error = null)
    }

    fun setName(value: String) {
        _ui.value = _ui.value.copy(name = value, error = null)
    }

    fun probe() {
        val url = _ui.value.baseUrl
        viewModelScope.launch {
            _ui.value = _ui.value.copy(probing = true, error = null)
            try {
                val config = repository.probe(url)
                _ui.value = _ui.value.copy(
                    probing = false,
                    config = config,
                    email = config.loginHint.ifBlank { _ui.value.email },
                )
                if (config.autoLogin) {
                    repository.openFamily(url)
                }
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(probing = false, error = error.message)
            }
        }
    }

    fun login() {
        val state = _ui.value
        viewModelScope.launch {
            _ui.value = state.copy(submitting = true, error = null)
            try {
                repository.login(state.baseUrl, state.email, state.password)
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(submitting = false, error = error.message)
            }
        }
    }

    fun register() {
        val state = _ui.value
        viewModelScope.launch {
            _ui.value = state.copy(submitting = true, error = null)
            try {
                repository.register(state.baseUrl, state.name, state.email, state.password)
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(submitting = false, error = error.message)
            }
        }
    }
}
