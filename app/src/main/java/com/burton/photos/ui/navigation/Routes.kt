package com.burton.photos.ui.navigation

import android.net.Uri
import com.burton.photos.domain.PhotoQuery

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LIBRARY = "library"
    const val ALBUMS = "albums"
    const val FAVORITES = "favorites"
    const val MORE = "more"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ARCHIVE = "archive"
    const val FOLDERS = "folders"
    const val LABELS = "labels"
    const val PEOPLE = "people"
    const val MOMENTS = "moments"
    const val CALENDAR = "calendar"
    const val PHOTO = "photo/{photoId}"
    const val ALBUM = "album/{albumId}"
    const val EDIT = "edit/{photoId}"
    const val BROWSE =
        "browse?title={title}&q={q}&year={year}&month={month}&city={city}&folder={folder}&label={label}&person={person}&album={album}&favorite={favorite}&archived={archived}"

    fun photo(id: String): String = "photo/${Uri.encode(id)}"
    fun album(id: String): String = "album/${Uri.encode(id)}"
    fun edit(id: String): String = "edit/${Uri.encode(id)}"

    fun browse(title: String, query: PhotoQuery): String {
        fun enc(value: String?) = Uri.encode(value.orEmpty())
        return buildString {
            append("browse?title=${enc(title)}")
            append("&q=${enc(query.q)}")
            append("&year=${enc(query.year)}")
            append("&month=${enc(query.month)}")
            append("&city=${enc(query.city)}")
            append("&folder=${enc(query.folder)}")
            append("&label=${enc(query.label)}")
            append("&person=${enc(query.person)}")
            append("&album=${enc(query.album)}")
            append("&favorite=${if (query.favorite == true) "true" else ""}")
            append("&archived=${if (query.archived == true) "true" else ""}")
        }
    }

    fun queryFrom(args: android.os.Bundle?): PhotoQuery {
        if (args == null) return PhotoQuery()
        fun v(key: String) = args.getString(key)?.takeIf { it.isNotBlank() }
        return PhotoQuery(
            q = v("q"),
            year = v("year"),
            month = v("month"),
            city = v("city"),
            folder = v("folder"),
            label = v("label"),
            person = v("person"),
            album = v("album"),
            favorite = if (v("favorite") == "true") true else null,
            archived = if (v("archived") == "true") true else null,
            title = v("title"),
        )
    }
}

val BottomTabs = listOf(Routes.LIBRARY, Routes.ALBUMS, Routes.FAVORITES, Routes.MORE)
