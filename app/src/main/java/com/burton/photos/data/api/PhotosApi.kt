package com.burton.photos.data.api

import com.burton.photos.data.parse.PhotosJson
import com.burton.photos.data.parse.TinyJson
import com.burton.photos.domain.Album
import com.burton.photos.domain.AuthConfig
import com.burton.photos.domain.CalendarYear
import com.burton.photos.domain.CoverItem
import com.burton.photos.domain.Health
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoPage
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.User
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(message: String, val code: Int) : Exception(message)

data class AuthResult(
    val user: User,
    val token: String?,
    val mode: String?,
)

@Singleton
class PhotosApi @Inject constructor(
    private val client: OkHttpClient,
    private val session: SessionStore,
) {
    fun authConfig(): AuthConfig = PhotosJson.authConfig(get("/api/auth/config"))

    fun login(email: String, password: String): AuthResult {
        val body = TinyJson.stringify(mapOf("email" to email, "password" to password))
        return PhotosJson.session(post("/api/auth/login", body)).toAuth()
    }

    fun register(name: String, email: String, password: String): AuthResult {
        val payload = mutableMapOf<String, Any?>("email" to email, "password" to password)
        if (name.isNotBlank()) payload["name"] = name
        return PhotosJson.session(post("/api/auth/register", TinyJson.stringify(payload))).toAuth()
    }

    fun me(): AuthResult = PhotosJson.session(get("/api/auth/me")).toAuth()

    fun logout() {
        post("/api/auth/logout", "{}")
    }

    fun health(): Health = PhotosJson.health(get("/api/health"))

    fun photos(query: PhotoQuery, limit: Int, offset: Int): PhotoPage {
        val url = url("/api/photos") {
            query.queryPairs().forEach { (key, value) -> addQueryParameter(key, value) }
            addQueryParameter("limit", limit.toString())
            addQueryParameter("offset", offset.toString())
        }
        return PhotosJson.photoPage(execute(Request.Builder().url(url).get().build()))
    }

    fun photo(id: String): Photo = PhotosJson.photoFromBody(get("/api/photos/$id"))

    fun patchPhoto(id: String, body: Map<String, Any?>): Photo =
        PhotosJson.photoFromBody(patch("/api/photos/$id", TinyJson.stringify(body)))

    fun upload(fileName: String, bytes: ByteArray, mime: String): Photo {
        val part = MultipartBody.Part.createFormData(
            "file",
            fileName,
            bytes.toRequestBody(mime.toMediaType()),
        )
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(part)
            .build()
        val request = Request.Builder().url(url("/api/photos/upload")).post(body).build()
        return PhotosJson.photoFromBody(execute(request))
    }

    fun albums(): List<Album> = PhotosJson.albums(get("/api/albums"))

    fun album(id: String): Album = PhotosJson.albumFromBody(get("/api/albums/$id"))

    fun createAlbum(title: String, description: String = ""): Album {
        val payload = mapOf("title" to title, "description" to description)
        return PhotosJson.albumFromBody(post("/api/albums", TinyJson.stringify(payload)))
    }

    fun folders(): List<CoverItem> = PhotosJson.folders(get("/api/library/folders"))

    fun labels(): List<CoverItem> = PhotosJson.labels(get("/api/library/labels"))

    fun people(): List<CoverItem> = PhotosJson.people(get("/api/library/people"))

    fun moments(): List<CoverItem> = PhotosJson.moments(get("/api/library/moments"))

    fun calendar(): List<CalendarYear> = PhotosJson.calendar(get("/api/library/calendar"))

    private fun Triple<User, String?, String?>.toAuth() = AuthResult(first, second, third)

    private fun get(path: String): String =
        execute(Request.Builder().url(url(path)).get().build())

    private fun post(path: String, json: String): String =
        execute(
            Request.Builder()
                .url(url(path))
                .post(json.toRequestBody(JSON))
                .build(),
        )

    private fun patch(path: String, json: String): String =
        execute(
            Request.Builder()
                .url(url(path))
                .patch(json.toRequestBody(JSON))
                .build(),
        )

    private fun url(path: String, extra: okhttp3.HttpUrl.Builder.() -> Unit = {}): okhttp3.HttpUrl {
        val origin = session.origin()
        check(origin.isNotBlank()) { "Server URL is not set" }
        val builder = origin.toHttpUrl().newBuilder()
        path.trimStart('/').split('/').filter { it.isNotEmpty() }.forEach(builder::addPathSegment)
        builder.extra()
        return builder.build()
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(
                    PhotosJson.errorMessage(text, response.message.ifBlank { "HTTP ${response.code}" }),
                    response.code,
                )
            }
            return text
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
