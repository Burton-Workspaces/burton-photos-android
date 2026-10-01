package com.burton.photos.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.ui.components.AuthImage
import com.burton.photos.ui.components.ScreenMessage
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute
import com.burton.photos.ui.theme.BurtonSand

@Composable
fun PhotoScreen(
    onBack: () -> Unit,
    viewModel: PhotoViewModel = hiltViewModel(),
) {
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val current = photo
    if (current == null) {
        ScreenMessage(error ?: "Loading…")
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
            }
            Row {
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (current.favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (current.favorite) BurtonSand else BurtonIvory,
                    )
                }
                IconButton(onClick = viewModel::toggleArchive) {
                    Icon(
                        if (current.archived) Icons.Rounded.Unarchive else Icons.Outlined.Archive,
                        contentDescription = "Archive",
                        tint = BurtonIvory,
                    )
                }
            }
        }
        AuthImage(
            url = viewModel.mediaUrl(current.thumbXlUrl ?: current.originalUrl),
            contentDescription = current.title,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentScale = ContentScale.Fit,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(current.title.ifBlank { current.filename }, color = BurtonIvory)
            meta(current.takenAt)
            meta(listOfNotNull(current.cameraMake, current.cameraModel).joinToString(" "))
            meta(current.lens)
            meta(listOfNotNull(current.city, current.country).joinToString(", "))
            if (current.iso != null) meta("ISO ${current.iso}")
            if (current.labels.isNotEmpty()) meta(current.labels.joinToString { it.name })
            if (current.people.isNotEmpty()) meta(current.people.joinToString { it.name })
        }
    }
}

@Composable
private fun meta(text: String?) {
    if (!text.isNullOrBlank()) {
        Text(text, color = BurtonMute, modifier = Modifier.padding(top = 4.dp))
    }
}
