package com.burton.photos.data.upload

import android.net.Uri
import com.burton.photos.data.api.PhotosApi
import com.burton.photos.domain.Photo
import com.burton.photos.domain.UploadJob
import com.burton.photos.domain.UploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadQueue @Inject constructor(
    private val api: PhotosApi,
    private val converter: JpegConverter,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _jobs = MutableStateFlow<List<UploadJob>>(emptyList())
    val jobs: StateFlow<List<UploadJob>> = _jobs

    fun enqueue(uris: List<Uri>) {
        val added = uris.map { uri ->
            UploadJob(
                id = UUID.randomUUID().toString(),
                displayName = uri.lastPathSegment ?: "photo",
                uri = uri.toString(),
                status = UploadStatus.Queued,
            )
        }
        _jobs.update { it + added }
        scope.launch { drain() }
    }

    fun clearFinished() {
        _jobs.update { jobs -> jobs.filter { it.status == UploadStatus.Queued || it.status == UploadStatus.Running } }
    }

    private suspend fun drain() {
        mutex.withLock {
            while (true) {
                val next = _jobs.value.firstOrNull { it.status == UploadStatus.Queued } ?: return
                set(next.id) { it.copy(status = UploadStatus.Running, progress = 0.1f) }
                try {
                    val prepared = converter.prepare(Uri.parse(next.uri))
                    set(next.id) { it.copy(displayName = prepared.fileName, progress = 0.4f) }
                    val photo: Photo = api.upload(prepared.fileName, prepared.bytes, prepared.mime)
                    set(next.id) {
                        it.copy(
                            status = UploadStatus.Done,
                            progress = 1f,
                            displayName = photo.title.ifBlank { prepared.fileName },
                        )
                    }
                } catch (error: Exception) {
                    set(next.id) {
                        it.copy(
                            status = UploadStatus.Error,
                            progress = 0f,
                            error = error.message ?: "Upload failed",
                        )
                    }
                }
            }
        }
    }

    private fun set(id: String, transform: (UploadJob) -> UploadJob) {
        _jobs.update { jobs -> jobs.map { if (it.id == id) transform(it) else it } }
    }
}
