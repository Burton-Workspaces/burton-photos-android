package com.burton.photos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.burton.photos.domain.Photo
import com.burton.photos.ui.theme.BurtonCharcoal
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute
import com.burton.photos.ui.theme.BurtonSand
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun AuthImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isNullOrBlank()) {
        Box(modifier.background(BurtonCharcoal))
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
fun PhotoGrid(
    photos: List<Photo>,
    mediaUrl: (String?) -> String?,
    onPhoto: (Photo) -> Unit,
    loading: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyGridState()
    LaunchedEffect(state, photos.size) {
        snapshotFlow {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= photos.size - 6
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }
    if (loading && photos.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BurtonSand)
        }
        return
    }
    if (photos.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = BurtonMute)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { _, photo ->
            AuthImage(
                url = mediaUrl(photo.thumbSmUrl ?: photo.thumbUrl),
                contentDescription = photo.title,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhoto(photo) },
            )
        }
        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BurtonSand)
                }
            }
        }
    }
}

@Composable
fun ScreenMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = BurtonIvory)
    }
}
