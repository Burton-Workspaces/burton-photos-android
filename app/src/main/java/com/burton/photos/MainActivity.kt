package com.burton.photos

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.photos.data.repository.PhotosRepository
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.SessionState
import com.burton.photos.ui.albums.AlbumDetailScreen
import com.burton.photos.ui.albums.AlbumsScreen
import com.burton.photos.ui.browse.BrowseKind
import com.burton.photos.ui.browse.CalendarScreen
import com.burton.photos.ui.browse.CoverBrowseScreen
import com.burton.photos.ui.browse.MoreScreen
import com.burton.photos.ui.browse.browseRoute
import com.burton.photos.ui.browse.monthRoute
import com.burton.photos.ui.library.LibraryScreen
import com.burton.photos.ui.login.LoginScreen
import com.burton.photos.ui.login.RegisterScreen
import com.burton.photos.ui.navigation.BottomTabs
import com.burton.photos.ui.navigation.Routes
import com.burton.photos.ui.search.SearchScreen
import com.burton.photos.ui.settings.SettingsScreen
import com.burton.photos.ui.theme.BurtonBlack
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute
import com.burton.photos.ui.theme.BurtonPhotosTheme
import com.burton.photos.ui.theme.BurtonSand
import com.burton.photos.ui.viewer.PhotoScreen
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var repository: PhotosRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BurtonPhotosTheme {
                BurtonApp(onEnqueue = { repository.uploads.enqueue(it) })
            }
        }
    }
}

@Composable
private fun BurtonApp(
    onEnqueue: (List<Uri>) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    when (session) {
        SessionState.Unknown -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurtonSand)
            }
        }
        is SessionState.SignedOut -> {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = Routes.LOGIN) {
                composable(Routes.LOGIN) {
                    LoginScreen(onRegister = { nav.navigate(Routes.REGISTER) })
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(onBack = { nav.popBackStack() })
                }
            }
        }
        is SessionState.SignedIn -> SignedInApp(onEnqueue)
    }
}

@Composable
private fun SignedInApp(onEnqueue: (List<Uri>) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in BottomTabs
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) onEnqueue(uris) }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok -> if (ok) captureUri?.let { onEnqueue(listOf(it)) } }

    fun openCamera() {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File.createTempFile("capture-", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        captureUri = uri
        camera.launch(uri)
    }

    Scaffold(
        containerColor = BurtonBlack,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = BurtonBlack) {
                    BottomTab(Routes.LIBRARY, "Library", Icons.Outlined.PhotoLibrary, route, navController)
                    BottomTab(Routes.ALBUMS, "Albums", Icons.Outlined.Collections, route, navController)
                    BottomTab(Routes.FAVORITES, "Favorites", Icons.Outlined.FavoriteBorder, route, navController)
                    BottomTab(Routes.MORE, "More", Icons.Outlined.MoreHoriz, route, navController)
                }
            }
        },
        floatingActionButton = {
            if (showBar) {
                FloatingActionButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    containerColor = BurtonSand,
                    contentColor = BurtonBlack,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Upload")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIBRARY) {
                Column(Modifier.fillMaxSize()) {
                    LibraryHeader(
                        onSearch = { navController.navigate(Routes.SEARCH) },
                        onCamera = ::openCamera,
                    )
                    LibraryScreen(
                        query = PhotoQuery(),
                        title = "",
                        onPhoto = { navController.navigate(Routes.photo(it.id)) },
                    )
                }
            }
            composable(Routes.ALBUMS) {
                AlbumsScreen(onAlbum = { navController.navigate(Routes.album(it)) })
            }
            composable(Routes.FAVORITES) {
                LibraryScreen(
                    query = PhotoQuery(favorite = true),
                    title = "Favorites",
                    onPhoto = { navController.navigate(Routes.photo(it.id)) },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(onRoute = { navController.navigate(it) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onPhoto = { navController.navigate(Routes.photo(it.id)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.ARCHIVE) {
                LibraryScreen(
                    query = PhotoQuery(archived = true),
                    title = "Archive",
                    onPhoto = { navController.navigate(Routes.photo(it.id)) },
                )
            }
            composable(Routes.FOLDERS) {
                CoverBrowseScreen(title = "Folders", kind = BrowseKind.Folders) {
                    navController.navigate(it.browseRoute())
                }
            }
            composable(Routes.LABELS) {
                CoverBrowseScreen(title = "Labels", kind = BrowseKind.Labels) {
                    navController.navigate(it.browseRoute())
                }
            }
            composable(Routes.PEOPLE) {
                CoverBrowseScreen(title = "People", kind = BrowseKind.People) {
                    navController.navigate(it.browseRoute())
                }
            }
            composable(Routes.MOMENTS) {
                CoverBrowseScreen(title = "Moments", kind = BrowseKind.Moments) {
                    navController.navigate(it.browseRoute())
                }
            }
            composable(Routes.CALENDAR) {
                CalendarScreen { year, month -> navController.navigate(monthRoute(year, month)) }
            }
            composable(
                Routes.PHOTO,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                PhotoScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.ALBUM,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                AlbumDetailScreen(onPhoto = { navController.navigate(Routes.photo(it.id)) })
            }
            composable(
                Routes.BROWSE,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    navArgument("year") { type = NavType.StringType; defaultValue = "" },
                    navArgument("month") { type = NavType.StringType; defaultValue = "" },
                    navArgument("city") { type = NavType.StringType; defaultValue = "" },
                    navArgument("folder") { type = NavType.StringType; defaultValue = "" },
                    navArgument("label") { type = NavType.StringType; defaultValue = "" },
                    navArgument("person") { type = NavType.StringType; defaultValue = "" },
                    navArgument("album") { type = NavType.StringType; defaultValue = "" },
                    navArgument("favorite") { type = NavType.StringType; defaultValue = "" },
                    navArgument("archived") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val query = Routes.queryFrom(entry.arguments)
                LibraryScreen(
                    query = query,
                    title = query.title ?: "Library",
                    onPhoto = { navController.navigate(Routes.photo(it.id)) },
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    onSearch: () -> Unit,
    onCamera: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Library", color = BurtonIvory, modifier = Modifier.padding(start = 8.dp).weight(1f))
        IconButton(onClick = onCamera) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera", tint = BurtonIvory)
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = BurtonIvory)
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    dest: String,
    label: String,
    icon: ImageVector,
    current: String?,
    nav: NavHostController,
) {
    NavigationBarItem(
        selected = current == dest,
        onClick = {
            nav.navigate(dest) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BurtonSand,
            selectedTextColor = BurtonSand,
            unselectedIconColor = BurtonMute,
            unselectedTextColor = BurtonMute,
            indicatorColor = Color.Transparent,
        ),
    )
}
