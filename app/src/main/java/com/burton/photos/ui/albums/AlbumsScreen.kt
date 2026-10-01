package com.burton.photos.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.domain.Photo
import com.burton.photos.ui.components.AuthImage
import com.burton.photos.ui.components.PhotoGrid
import com.burton.photos.ui.components.ScreenMessage
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute

@Composable
fun AlbumsScreen(
    onAlbum: (String) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text("Albums", color = BurtonIvory, modifier = Modifier.padding(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("New album") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = {
                viewModel.create(title)
                title = ""
            }) {
                Text("Create")
            }
        }
        error?.let { Text(it, color = BurtonMute, modifier = Modifier.padding(16.dp)) }
        if (albums.isEmpty()) {
            ScreenMessage("No albums yet")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    Column(Modifier.clickable { onAlbum(album.id) }) {
                        AuthImage(
                            url = viewModel.mediaUrl(album.coverUrl),
                            contentDescription = album.title,
                            modifier = Modifier.aspectRatio(1f),
                        )
                        Text(album.title, color = BurtonIvory, modifier = Modifier.padding(top = 6.dp))
                        Text("${album.photoCount} photos", color = BurtonMute)
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    onPhoto: (Photo) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val current = album
    if (current == null) {
        ScreenMessage(error ?: "Loading…")
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text(current.title, color = BurtonIvory, modifier = Modifier.padding(16.dp))
        PhotoGrid(
            photos = current.photos,
            mediaUrl = viewModel::mediaUrl,
            onPhoto = onPhoto,
            loading = false,
            loadingMore = false,
            onLoadMore = {},
            emptyText = "Empty album",
        )
    }
}
