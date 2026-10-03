package com.burton.photos.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.UploadStatus
import com.burton.photos.ui.components.PhotoGrid
import com.burton.photos.ui.local.MediaAccessGate
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute
import com.burton.photos.ui.theme.BurtonSand

@Composable
fun LibraryScreen(
    query: PhotoQuery,
    title: String,
    viewModel: LibraryViewModel = hiltViewModel(),
    onPhoto: (Photo) -> Unit,
    onConnect: (() -> Unit)? = null,
) {
    val snap by viewModel.library.collectAsStateWithLifecycle()
    val uploads by viewModel.uploads.collectAsStateWithLifecycle()
    val local by viewModel.local.collectAsStateWithLifecycle()
    MediaAccessGate(required = local, onConnect = onConnect) {
        LaunchedEffect(query) { viewModel.load(query) }
        val pending = uploads.filter { it.status == UploadStatus.Queued || it.status == UploadStatus.Running }
        Column(Modifier.fillMaxSize()) {
            if (title.isNotBlank()) {
                Text(title, color = BurtonIvory, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            if (pending.isNotEmpty()) {
                val active = pending.first()
                Text(
                    "Uploading ${active.displayName} (${pending.size} left)",
                    color = BurtonMute,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LinearProgressIndicator(
                    progress = { active.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = BurtonSand,
                )
            }
            snap.error?.let {
                Text(it, color = BurtonMute, modifier = Modifier.padding(16.dp))
            }
            PhotoGrid(
                photos = snap.photos,
                mediaUrl = viewModel::mediaUrl,
                onPhoto = onPhoto,
                loading = snap.loading,
                loadingMore = snap.loadingMore,
                onLoadMore = viewModel::loadMore,
                emptyText = if (local) "No photos on this phone" else "No photos yet",
            )
        }
    }
}
