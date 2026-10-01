package com.burton.photos.data.parse

import com.burton.photos.data.parse.TinyJson.bool
import com.burton.photos.data.parse.TinyJson.dblOrNull
import com.burton.photos.data.parse.TinyJson.int
import com.burton.photos.data.parse.TinyJson.intOrNull
import com.burton.photos.data.parse.TinyJson.long
import com.burton.photos.data.parse.TinyJson.obj
import com.burton.photos.data.parse.TinyJson.objList
import com.burton.photos.data.parse.TinyJson.parseObject
import com.burton.photos.data.parse.TinyJson.str
import com.burton.photos.data.parse.TinyJson.strOrNull
import com.burton.photos.domain.Album
import com.burton.photos.domain.AuthConfig
import com.burton.photos.domain.CalendarMonth
import com.burton.photos.domain.CalendarYear
import com.burton.photos.domain.CoverItem
import com.burton.photos.domain.Health
import com.burton.photos.domain.NamedId
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoPage
import com.burton.photos.domain.PhotoQuery
import com.burton.photos.domain.User

object PhotosJson {
    fun errorMessage(text: String, fallback: String): String {
        val parsed = runCatching { parseObject(text) }.getOrNull() ?: return fallback
        return parsed.strOrNull("error")?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun authConfig(text: String): AuthConfig {
        val o = parseObject(text)
        return AuthConfig(
            mode = o.str("mode", "accounts"),
            register = o.bool("register", true),
            autoLogin = o.bool("autoLogin"),
            loginHint = o.str("loginHint"),
        )
    }

    fun user(map: Map<String, Any?>): User =
        User(
            id = map.str("id"),
            email = map.str("email"),
            name = map.str("name"),
            role = map.str("role", "user"),
        )

    fun session(text: String): Triple<User, String?, String?> {
        val o = parseObject(text)
        return Triple(user(o.obj("user")), o.strOrNull("token"), o.strOrNull("mode"))
    }

    fun health(text: String): Health {
        val o = parseObject(text)
        val checks = o.obj("checks")
        return Health(
            ok = o.bool("ok"),
            name = o.str("name", "burton-photos"),
            checks = mapOf(
                "api" to checks.bool("api"),
                "database" to checks.bool("database"),
                "engine" to checks.bool("engine"),
                "originals" to checks.bool("originals"),
            ),
        )
    }

    fun photo(map: Map<String, Any?>): Photo =
        Photo(
            id = map.str("id"),
            ownerId = map.str("ownerId"),
            groupId = map.strOrNull("groupId"),
            filename = map.str("filename"),
            folder = map.str("folder"),
            title = map.str("title"),
            description = map.str("description"),
            mime = map.str("mime"),
            width = map.int("width"),
            height = map.int("height"),
            size = map.long("size"),
            takenAt = map.strOrNull("takenAt"),
            cameraMake = map.strOrNull("cameraMake"),
            cameraModel = map.strOrNull("cameraModel"),
            lens = map.strOrNull("lens"),
            iso = map.intOrNull("iso"),
            aperture = map.dblOrNull("aperture"),
            shutter = map.strOrNull("shutter"),
            focalLength = map.dblOrNull("focalLength"),
            lat = map.dblOrNull("lat"),
            lng = map.dblOrNull("lng"),
            country = map.strOrNull("country"),
            city = map.strOrNull("city"),
            favorite = map.bool("favorite"),
            private = map.bool("private"),
            archived = map.bool("archived"),
            thumbUrl = map.strOrNull("thumbUrl"),
            thumbSmUrl = map.strOrNull("thumbSmUrl"),
            thumbXlUrl = map.strOrNull("thumbXlUrl"),
            originalUrl = map.str("originalUrl"),
            labels = map.objList("labels").map {
                NamedId(it.str("id"), it.str("name"), it.strOrNull("slug"))
            },
            people = map.objList("people").map { NamedId(it.str("id"), it.str("name")) },
            albums = map.objList("albums").map { NamedId(it.str("id"), it.str("title")) },
        )

    fun photoFromBody(text: String): Photo = photo(parseObject(text).obj("photo"))

    fun photoPage(text: String): PhotoPage {
        val o = parseObject(text)
        return PhotoPage(
            photos = o.objList("photos").map(::photo),
            total = o.int("total"),
            limit = o.int("limit", 120),
            offset = o.int("offset"),
        )
    }

    fun albums(text: String): List<Album> =
        parseObject(text).objList("albums").map(::album)

    fun albumFromBody(text: String): Album = album(parseObject(text).obj("album"))

    fun album(map: Map<String, Any?>): Album =
        Album(
            id = map.str("id"),
            ownerId = map.str("ownerId"),
            title = map.str("title"),
            description = map.str("description"),
            coverPhotoId = map.strOrNull("coverPhotoId"),
            createdAt = map.str("createdAt"),
            photoCount = map.int("photoCount"),
            coverUrl = map.strOrNull("coverUrl"),
            shares = map.objList("shares").map { NamedId(it.str("id"), it.str("name")) },
            photos = map.objList("photos").map(::photo),
        )

    fun folders(text: String): List<CoverItem> =
        parseObject(text).objList("folders").map {
            val folder = it.str("folder")
            CoverItem(
                id = folder,
                title = folder.ifBlank { "Root" },
                subtitle = "",
                count = it.int("count"),
                coverUrl = it.strOrNull("coverUrl"),
                query = PhotoQuery(folder = folder),
            )
        }

    fun labels(text: String): List<CoverItem> =
        parseObject(text).objList("labels").map {
            CoverItem(
                id = it.str("id"),
                title = it.str("name"),
                subtitle = it.str("slug"),
                count = it.int("count"),
                coverUrl = it.strOrNull("coverUrl"),
                query = PhotoQuery(label = it.str("id")),
            )
        }

    fun people(text: String): List<CoverItem> =
        parseObject(text).objList("people").map {
            CoverItem(
                id = it.str("id"),
                title = it.str("name"),
                subtitle = "",
                count = it.int("count"),
                coverUrl = it.strOrNull("coverUrl"),
                query = PhotoQuery(person = it.str("id")),
            )
        }

    fun moments(text: String): List<CoverItem> =
        parseObject(text).objList("moments").map {
            CoverItem(
                id = "${it.str("city")}-${it.str("year")}",
                title = it.str("title"),
                subtitle = listOf(it.strOrNull("country"), it.strOrNull("year")).filterNotNull().joinToString(" · "),
                count = it.int("count"),
                coverUrl = it.strOrNull("coverUrl"),
                query = PhotoQuery(city = it.strOrNull("city"), year = it.strOrNull("year")),
            )
        }

    fun calendar(text: String): List<CalendarYear> =
        parseObject(text).objList("years").map { year ->
            CalendarYear(
                year = year.str("year"),
                count = year.int("count"),
                months = year.objList("months").map {
                    CalendarMonth(it.str("month"), it.int("count"))
                },
            )
        }
}
