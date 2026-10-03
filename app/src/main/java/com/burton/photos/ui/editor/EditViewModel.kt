package com.burton.photos.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.photos.data.edit.CropWindow
import com.burton.photos.data.edit.EditedPhotoStore
import com.burton.photos.data.edit.PhotoEdits
import com.burton.photos.data.edit.PhotoFilter
import com.burton.photos.data.local.LocalIds
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotosRepository,
    private val store: EditedPhotoStore,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["photoId"])
    private var source: Bitmap? = null
    private var previewJob: Job? = null

    private val _photo = MutableStateFlow<Photo?>(null)
    val photo: StateFlow<Photo?> = _photo
    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview
    private val _edits = MutableStateFlow(PhotoEdits())
    val edits: StateFlow<PhotoEdits> = _edits
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            try {
                val photo = repository.photo(id)
                _photo.value = photo
                val url = repository.mediaUrl(photo.originalUrl) ?: photo.originalUrl
                val bitmap = store.load(url)
                source = bitmap
                _preview.value = bitmap
            } catch (error: Exception) {
                _error.value = error.message
            }
        }
    }

    fun rotateLeft() = setGeometry { it.rotateBy(-90) }
    fun rotateRight() = setGeometry { it.rotateBy(90) }
    fun flipHorizontal() = setGeometry { it.copy(flipHorizontal = !it.flipHorizontal) }
    fun flipVertical() = setGeometry { it.copy(flipVertical = !it.flipVertical) }

    fun setBrightness(value: Float) = updateColor { it.copy(brightness = value) }
    fun setContrast(value: Float) = updateColor { it.copy(contrast = value) }
    fun setSaturation(value: Float) = updateColor { it.copy(saturation = value) }
    fun setWarmth(value: Float) = updateColor { it.copy(warmth = value) }
    fun setFilter(filter: PhotoFilter) = updateColor { it.copy(filter = filter) }

    fun reset() {
        _edits.value = PhotoEdits()
        _error.value = null
        rebuildPreview()
    }

    fun save(crop: CropWindow, onSaved: (String) -> Unit) {
        val src = source ?: return
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                val exported = withContext(Dispatchers.Default) {
                    store.export(src, _edits.value, crop)
                }
                val name = store.editedFileName(_photo.value?.filename ?: "photo.jpg")
                val mediaId = store.saveJpeg(exported, name)
                if (exported !== src) exported.recycle()
                if (repository.isLocal) {
                    repository.refreshLibrary(repository.library.value.query)
                }
                onSaved(LocalIds.photo(mediaId))
            } catch (error: Exception) {
                _error.value = error.message
            } finally {
                _saving.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val src = source
        val shown = _preview.value
        if (shown != null && shown !== src) shown.recycle()
        src?.recycle()
        source = null
    }

    private fun updateColor(block: (PhotoEdits) -> PhotoEdits) {
        _edits.update(block)
    }

    private fun setGeometry(block: (PhotoEdits) -> PhotoEdits) {
        _edits.update(block)
        rebuildPreview()
    }

    private fun rebuildPreview() {
        val src = source ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            val next = store.oriented(src, _edits.value)
            withContext(Dispatchers.Main) {
                val old = _preview.value
                _preview.value = next
                if (old != null && old !== src && old !== next) old.recycle()
            }
        }
    }
}
