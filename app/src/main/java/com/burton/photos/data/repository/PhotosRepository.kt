package com.burton.photos.data.repository

import com.burton.photos.data.api.ApiException
import com.burton.photos.data.api.PhotosApi
import com.burton.photos.data.api.SessionStore
import com.burton.photos.data.local.LocalGallery
import com.burton.photos.data.local.LocalIds
import com.burton.photos.data.prefs.LocalPrefs
import com.burton.photos.data.upload.UploadQueue
import com.burton.photos.domain.Album
import com.burton.photos.domain.AuthConfig
import com.burton.photos.domain.CalendarYear
import com.burton.photos.domain.CoverItem
import com.burton.photos.domain.Health
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.SessionState
import com.burton.photos.domain.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class LibrarySnapshot(
    val query: PhotoQuery = PhotoQuery(),
    val photos: List<Photo> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

@Singleton
class PhotosRepository @Inject constructor(
    private val api: PhotosApi,
    private val prefs: LocalPrefs,
    private val session: SessionStore,
    private val local: LocalGallery,
    val uploads: UploadQueue,
) {
    private val _session = MutableStateFlow<SessionState>(SessionState.Unknown)
    val sessionState: StateFlow<SessionState> = _session

    private val _library = MutableStateFlow(LibrarySnapshot())
    val library: StateFlow<LibrarySnapshot> = _library

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    var lastConfig: AuthConfig? = null
        private set

    val isLocal: Boolean get() = _session.value.isLocal

    suspend fun start() {
        val stored = prefs.load()
        if (stored == null) {
            goLocal()
            return
        }
        session.baseUrl = stored.baseUrl
        session.token = stored.token
        try {
            val me = api.me()
            val token = me.token ?: stored.token
            session.token = token
            val mode = me.mode ?: stored.mode
            prefs.saveSession(stored.baseUrl, token, me.user, mode)
            _session.value = SessionState.SignedIn(me.user, mode)
            refreshLibrary(PhotoQuery())
            refreshAlbums()
        } catch (error: Exception) {
            if (error is ApiException && error.code == 401) {
                prefs.clearSession()
                session.token = null
                lastConfig = runCatching { api.authConfig() }.getOrNull()
                goLocal()
            } else if (stored.user != null) {
                _session.value = SessionState.SignedIn(stored.user, stored.mode)
            } else {
                goLocal()
            }
        }
    }

    suspend fun savedOrigin(): String = prefs.origin()

    suspend fun probe(baseUrl: String): AuthConfig {
        session.baseUrl = baseUrl.trim().trimEnd('/')
        prefs.saveBaseUrl(session.baseUrl)
        val config = api.authConfig()
        lastConfig = config
        return config
    }

    suspend fun login(baseUrl: String, email: String, password: String) {
        val config = probe(baseUrl)
        val result = if (config.autoLogin) api.me() else api.login(email, password)
        accept(result.user, result.token, result.mode ?: config.mode)
    }

    suspend fun register(baseUrl: String, name: String, email: String, password: String) {
        probe(baseUrl)
        val result = api.register(name, email, password)
        accept(result.user, result.token, result.mode ?: "accounts")
    }

    suspend fun openFamily(baseUrl: String) {
        val config = probe(baseUrl)
        if (!config.autoLogin) error("This server requires a login.")
        val result = api.me()
        accept(result.user, result.token, result.mode ?: config.mode)
    }

    suspend fun logout() {
        runCatching { api.logout() }
        prefs.clearSession()
        session.token = null
        goLocal()
    }

    suspend fun refreshLibrary(query: PhotoQuery) {
        _library.update { it.copy(query = query, loading = true, error = null, photos = emptyList(), total = 0) }
        try {
            val page = loadPage(query, PAGE, 0)
            _library.update {
                it.copy(photos = page.photos, total = page.total, loading = false, error = null)
            }
        } catch (error: Exception) {
            _library.update { it.copy(loading = false, error = error.message) }
        }
    }

    suspend fun loadMore() {
        val snap = _library.value
        if (snap.loading || snap.loadingMore || snap.photos.size >= snap.total) return
        _library.update { it.copy(loadingMore = true) }
        try {
            val page = loadPage(snap.query, PAGE, snap.photos.size)
            _library.update {
                it.copy(
                    photos = it.photos + page.photos,
                    total = page.total,
                    loadingMore = false,
                )
            }
        } catch (error: Exception) {
            _library.update { it.copy(loadingMore = false, error = error.message) }
        }
    }

    suspend fun photo(id: String): Photo = when {
        LocalIds.isPhoto(id) -> local.photo(id)
        useLocal -> error("Photo not found")
        else -> api.photo(id)
    }

    suspend fun toggleFavorite(photo: Photo): Photo {
        requireServer()
        val next = api.patchPhoto(photo.id, mapOf("favorite" to !photo.favorite))
        replace(next)
        return next
    }

    suspend fun toggleArchive(photo: Photo): Photo {
        requireServer()
        val next = api.patchPhoto(photo.id, mapOf("archived" to !photo.archived))
        replace(next)
        return next
    }

    suspend fun refreshAlbums() {
        _albums.value = runCatching {
            if (useLocal) local.albums() else api.albums()
        }.getOrDefault(emptyList())
    }

    suspend fun album(id: String): Album =
        if (useLocal || LocalIds.isBucket(id)) local.album(id) else api.album(id)

    suspend fun createAlbum(title: String): Album {
        requireServer()
        val album = api.createAlbum(title)
        refreshAlbums()
        return album
    }

    suspend fun folders(): List<CoverItem> = if (useLocal) local.folders() else api.folders()
    suspend fun labels(): List<CoverItem> = serverOnly { api.labels() }
    suspend fun people(): List<CoverItem> = serverOnly { api.people() }
    suspend fun moments(): List<CoverItem> = serverOnly { api.moments() }
    suspend fun calendar(): List<CalendarYear> = if (useLocal) local.calendar() else api.calendar()
    suspend fun health(): Health {
        requireServer()
        return api.health()
    }

    fun mediaUrl(path: String?): String? = session.absolute(path)

    private suspend fun accept(user: User, token: String?, mode: String) {
        val resolved = token ?: error("Server did not return a session token. Update Burton Photos.")
        session.token = resolved
        prefs.saveSession(session.origin(), resolved, user, mode)
        _session.value = SessionState.SignedIn(user, mode)
        refreshLibrary(PhotoQuery())
        refreshAlbums()
    }

    private suspend fun goLocal() {
        _session.value = SessionState.Local
        _library.value = LibrarySnapshot()
        _albums.value = emptyList()
        runCatching { refreshAlbums() }
    }

    private suspend fun loadPage(query: PhotoQuery, limit: Int, offset: Int) =
        if (useLocal) local.photos(query, limit, offset) else api.photos(query, limit, offset)

    private val useLocal: Boolean get() = _session.value.isLocal

    private fun requireServer() {
        if (useLocal) error("Connect to a Burton Photos server to use this.")
    }

    private suspend fun <T> serverOnly(block: suspend () -> T): T {
        requireServer()
        return block()
    }

    private fun replace(photo: Photo) {
        _library.update { snap ->
            snap.copy(photos = snap.photos.map { if (it.id == photo.id) photo else it })
        }
    }

    companion object {
        private const val PAGE = 120
    }
}
