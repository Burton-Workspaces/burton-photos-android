package com.burton.photos.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.ui.library.LibraryScreen
import com.burton.photos.ui.theme.BurtonIvory

@Composable
fun SearchScreen(onPhoto: (Photo) -> Unit, onConnect: (() -> Unit)? = null) {
    var q by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text("Search", color = BurtonIvory, modifier = Modifier.padding(16.dp))
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            label = { Text("Find photos") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
        )
        if (q.length >= 2) {
            LibraryScreen(
                query = PhotoQuery(q = q),
                title = "Results",
                onPhoto = onPhoto,
                onConnect = onConnect,
            )
        }
    }
}
