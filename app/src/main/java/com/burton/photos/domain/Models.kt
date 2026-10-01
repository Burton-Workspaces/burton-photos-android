package com.burton.photos.domain

data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
)

data class AuthConfig(
    val mode: String,
    val register: Boolean,
    val autoLogin: Boolean,
    val loginHint: String,
)

data class Health(
    val ok: Boolean,
    val name: String,
    val checks: Map<String, Boolean>,
)

data class Photo(
    val id: String,
    val ownerId: String,
    val groupId: String?,
    val filename: String,
    val folder: String,
    val title: String,
    val description: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val takenAt: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val lens: String?,
    val iso: Int?,
    val aperture: Double?,
    val shutter: String?,
    val focalLength: Double?,
    val lat: Double?,
    val lng: Double?,
    val country: String?,
    val city: String?,
    val favorite: Boolean,
    val private: Boolean,
    val archived: Boolean,
    val thumbUrl: String?,
    val thumbSmUrl: String?,
    val thumbXlUrl: String?,
    val originalUrl: String,
    val labels: List<NamedId> = emptyList(),
    val people: List<NamedId> = emptyList(),
    val albums: List<NamedId> = emptyList(),
)

data class NamedId(
    val id: String,
    val name: String,
    val slug: String? = null,
)

data class PhotoPage(
    val photos: List<Photo>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

data class PhotoQuery(
    val q: String? = null,
    val year: String? = null,
    val month: String? = null,
    val camera: String? = null,
    val city: String? = null,
    val country: String? = null,
    val folder: String? = null,
    val label: String? = null,
    val person: String? = null,
    val album: String? = null,
    val favorite: Boolean? = null,
    val archived: Boolean? = null,
    val geo: Boolean? = null,
    val title: String? = null,
) {
    val isFiltered: Boolean
        get() = q != null || year != null || month != null || camera != null ||
            city != null || country != null || folder != null || label != null ||
            person != null || album != null || favorite != null || archived != null || geo != null

    fun queryPairs(): List<Pair<String, String>> = buildList {
        fun add(key: String, value: String?) {
            if (!value.isNullOrBlank()) add(key to value)
        }
        add("q", q)
        add("year", year)
        add("month", month)
        add("camera", camera)
        add("city", city)
        add("country", country)
        add("folder", folder)
        add("label", label)
        add("person", person)
        add("album", album)
        if (favorite == true) add("favorite" to "true")
        if (archived == true) add("archived" to "true")
        if (geo == true) add("geo" to "true")
    }
}

data class Album(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val coverPhotoId: String?,
    val createdAt: String,
    val photoCount: Int,
    val coverUrl: String?,
    val shares: List<NamedId>,
    val photos: List<Photo> = emptyList(),
)

data class CoverItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val count: Int,
    val coverUrl: String?,
    val query: PhotoQuery,
)

data class CalendarYear(
    val year: String,
    val count: Int,
    val months: List<CalendarMonth>,
)

data class CalendarMonth(
    val month: String,
    val count: Int,
)

enum class UploadStatus {
    Queued,
    Running,
    Done,
    Error,
}

data class UploadJob(
    val id: String,
    val displayName: String,
    val uri: String,
    val status: UploadStatus,
    val progress: Float = 0f,
    val error: String? = null,
)

sealed class SessionState {
    data object Unknown : SessionState()
    data class SignedOut(val config: AuthConfig?) : SessionState()
    data class SignedIn(val user: User, val mode: String) : SessionState()
}
