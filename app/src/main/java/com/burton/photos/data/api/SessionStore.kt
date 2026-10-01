package com.burton.photos.data.api

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor() {
    @Volatile
    var baseUrl: String = ""

    @Volatile
    var token: String? = null

    fun origin(): String = baseUrl.trimEnd('/')

    fun absolute(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val origin = origin()
        if (origin.isBlank()) return path
        return origin + if (path.startsWith("/")) path else "/$path"
    }
}
