package com.burton.photos.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.domain.CoverItem
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.ui.components.AuthImage
import com.burton.photos.ui.components.ScreenMessage
import com.burton.photos.ui.navigation.Routes
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonLine
import com.burton.photos.ui.theme.BurtonMute

@Composable
fun MoreScreen(onRoute: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("More", color = BurtonIvory, modifier = Modifier.padding(16.dp))
        moreRow("Folders", Icons.Outlined.Folder) { onRoute(Routes.FOLDERS) }
        moreRow("Labels", Icons.AutoMirrored.Outlined.Label) { onRoute(Routes.LABELS) }
        moreRow("People", Icons.Outlined.People) { onRoute(Routes.PEOPLE) }
        moreRow("Moments", Icons.Outlined.Place) { onRoute(Routes.MOMENTS) }
        moreRow("Calendar", Icons.Outlined.CalendarMonth) { onRoute(Routes.CALENDAR) }
        moreRow("Archive", Icons.Outlined.Archive) { onRoute(Routes.ARCHIVE) }
        moreRow("Settings", Icons.Outlined.Settings) { onRoute(Routes.SETTINGS) }
    }
}

@Composable
private fun moreRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = BurtonIvory) },
        leadingContent = { Icon(icon, contentDescription = null, tint = BurtonMute) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(color = BurtonLine)
}

@Composable
fun CoverBrowseScreen(
    title: String,
    kind: BrowseKind,
    viewModel: BrowseViewModel = hiltViewModel(),
    onItem: (CoverItem) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(kind) { viewModel.load(kind) }
    Column(Modifier.fillMaxSize()) {
        Text(title, color = BurtonIvory, modifier = Modifier.padding(16.dp))
        error?.let { Text(it, color = BurtonMute, modifier = Modifier.padding(16.dp)) }
        if (items.isEmpty() && error == null) {
            ScreenMessage("Nothing here yet")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Column(Modifier.clickable { onItem(item) }) {
                        AuthImage(
                            url = viewModel.mediaUrl(item.coverUrl),
                            contentDescription = item.title,
                            modifier = Modifier.aspectRatio(1f),
                        )
                        Text(item.title, color = BurtonIvory, modifier = Modifier.padding(top = 6.dp))
                        Text("${item.count} photos", color = BurtonMute)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    viewModel: BrowseViewModel = hiltViewModel(),
    onMonth: (year: String, month: String) -> Unit,
) {
    val years by viewModel.years.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadCalendar() }
    Column(Modifier.fillMaxSize()) {
        Text("Calendar", color = BurtonIvory, modifier = Modifier.padding(16.dp))
        error?.let { Text(it, color = BurtonMute, modifier = Modifier.padding(16.dp)) }
        LazyColumn(Modifier.fillMaxSize()) {
            items(years, key = { it.year }) { year ->
                Text(year.year, color = BurtonIvory, modifier = Modifier.padding(16.dp, 12.dp))
                year.months.forEach { month ->
                    ListItem(
                        headlineContent = { Text("Month ${month.month}", color = BurtonIvory) },
                        supportingContent = { Text("${month.count} photos", color = BurtonMute) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMonth(year.year, month.month) },
                    )
                }
            }
        }
    }
}

fun CoverItem.browseRoute(): String = Routes.browse(title, query)

fun monthRoute(year: String, month: String): String =
    Routes.browse("$year-$month", PhotoQuery(year = year, month = month))
