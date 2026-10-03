package com.burton.photos.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.burton.photos.domain.Album
import com.burton.photos.domain.CalendarMonth
import com.burton.photos.domain.CalendarYear
import com.burton.photos.domain.CoverItem
import com.burton.photos.domain.Photo
import com.burton.photos.domain.PhotoPage
import com.burton.photos.domain.PhotoQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalGallery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    suspend fun photos(query: PhotoQuery, limit: Int, offset: Int): PhotoPage = withContext(Dispatchers.IO) {
        if (query.archived == true || query.label != null || query.person != null) {
            return@withContext PhotoPage(emptyList(), 0, limit, offset)
        }
        if (query.favorite == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext PhotoPage(emptyList(), 0, limit, offset)
        }
        val folder = when {
            !query.folder.isNullOrBlank() -> query.folder
            query.album != null && LocalIds.isBucket(query.album) ->
                LocalIds.bucketId(query.album).toString()
            query.album?.all { it.isDigit() } == true -> query.album
            else -> query.folder
        }
        val filter = LocalMediaQuery.filter(
            query.copy(folder = folder, album = null),
            supportsFavorite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
        val total = count(filter)
        val rows = queryRows(filter, limit, offset)
        PhotoPage(rows.map { it.toPhoto() }, total, limit, offset)
    }

    suspend fun photo(id: String): Photo = withContext(Dispatchers.IO) {
        val mediaId = LocalIds.photoMediaId(id)
        val filter = SqlFilter("${MediaStore.Images.Media._ID} = ?", arrayOf(mediaId.toString()))
        val row = queryRows(filter, 1, 0).firstOrNull()
            ?: error("Photo not found")
        row.toPhoto()
    }

    suspend fun albums(): List<Album> = withContext(Dispatchers.IO) {
        buckets().map { it.toAlbum() }
    }

    suspend fun album(id: String): Album = withContext(Dispatchers.IO) {
        val bucketId = if (LocalIds.isBucket(id)) {
            LocalIds.bucketId(id)
        } else {
            id.toLongOrNull() ?: error("Album not found")
        }
        val bucket = buckets().firstOrNull { it.id == bucketId } ?: error("Album not found")
        val filter = LocalMediaQuery.filter(PhotoQuery(folder = bucketId.toString()))
        val photos = queryRows(filter, limit = null, offset = null).map { it.toPhoto() }
        bucket.toAlbum().copy(photos = photos, photoCount = photos.size)
    }

    suspend fun folders(): List<CoverItem> = withContext(Dispatchers.IO) {
        buckets().map { bucket ->
            CoverItem(
                id = LocalIds.bucket(bucket.id),
                title = bucket.name,
                subtitle = "",
                count = bucket.count,
                coverUrl = bucket.coverUri,
                query = PhotoQuery(folder = bucket.id.toString(), title = bucket.name),
            )
        }
    }

    suspend fun calendar(): List<CalendarYear> = withContext(Dispatchers.IO) {
        val counts = linkedMapOf<String, MutableMap<String, Int>>()
        queryDates().forEach { ms ->
            if (ms <= 0L) return@forEach
            val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
            val year = date.year.toString()
            val month = date.monthValue.toString().padStart(2, '0')
            val months = counts.getOrPut(year) { linkedMapOf() }
            months[month] = (months[month] ?: 0) + 1
        }
        counts.map { (year, months) ->
            CalendarYear(
                year = year,
                count = months.values.sum(),
                months = months.entries
                    .sortedBy { it.key }
                    .map { CalendarMonth(month = it.key, count = it.value) },
            )
        }.sortedByDescending { it.year }
    }

    private fun buckets(): List<Bucket> {
        val byId = linkedMapOf<Long, Bucket>()
        queryRows(null, limit = null, offset = null).forEach { row ->
            val current = byId[row.bucketId]
            if (current == null) {
                byId[row.bucketId] = Bucket(
                    id = row.bucketId,
                    name = row.bucketName.ifBlank { "Photos" },
                    count = 1,
                    coverUri = row.uri,
                    coverId = row.id,
                )
            } else {
                current.count += 1
            }
        }
        return byId.values.toList()
    }

    private fun count(filter: SqlFilter?): Int {
        val cursor = query(arrayOf(MediaStore.Images.Media._ID), filter, limit = null, offset = null)
            ?: return 0
        cursor.use { return it.count }
    }

    private fun queryDates(): List<Long> {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val cursor = query(projection, null, null, null) ?: return emptyList()
        val dates = ArrayList<Long>()
        cursor.use {
            val takenIdx = it.indexOf(MediaStore.Images.Media.DATE_TAKEN)
            val addedIdx = it.indexOf(MediaStore.Images.Media.DATE_ADDED)
            while (it.moveToNext()) {
                val taken = it.longOrZero(takenIdx)
                val added = it.longOrZero(addedIdx)
                dates += if (taken > 0L) taken else added * 1000L
            }
        }
        return dates
    }

    private fun queryRows(filter: SqlFilter?, limit: Int?, offset: Int?): List<LocalMediaRow> {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.BUCKET_ID)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
        }.toTypedArray()
        val cursor = query(projection, filter, limit, offset) ?: return emptyList()
        val rows = ArrayList<LocalMediaRow>()
        cursor.use {
            val idIdx = it.indexOf(MediaStore.Images.Media._ID)
            val nameIdx = it.indexOf(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeIdx = it.indexOf(MediaStore.Images.Media.MIME_TYPE)
            val widthIdx = it.indexOf(MediaStore.Images.Media.WIDTH)
            val heightIdx = it.indexOf(MediaStore.Images.Media.HEIGHT)
            val sizeIdx = it.indexOf(MediaStore.Images.Media.SIZE)
            val takenIdx = it.indexOf(MediaStore.Images.Media.DATE_TAKEN)
            val addedIdx = it.indexOf(MediaStore.Images.Media.DATE_ADDED)
            val bucketIdIdx = it.indexOf(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameIdx = it.indexOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val favoriteIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.indexOf(MediaStore.MediaColumns.IS_FAVORITE)
            } else {
                -1
            }
            while (it.moveToNext()) {
                val id = it.longOrZero(idIdx)
                rows += LocalMediaRow(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = it.stringOrEmpty(nameIdx),
                    mime = it.stringOrEmpty(mimeIdx),
                    width = it.intOrZero(widthIdx),
                    height = it.intOrZero(heightIdx),
                    size = it.longOrZero(sizeIdx),
                    dateTakenMs = it.longOrZero(takenIdx),
                    dateAddedSec = it.longOrZero(addedIdx),
                    bucketId = it.longOrZero(bucketIdIdx),
                    bucketName = it.stringOrEmpty(bucketNameIdx),
                    favorite = favoriteIdx >= 0 && it.intOrZero(favoriteIdx) != 0,
                )
            }
        }
        return rows
    }

    private fun query(
        projection: Array<String>,
        filter: SqlFilter?,
        limit: Int?,
        offset: Int?,
    ): Cursor? {
        val sortColumns = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val extras = Bundle().apply {
                filter?.let {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, it.selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it.args)
                }
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, sortColumns)
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
                )
                if (limit != null) putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                if (offset != null) putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            resolver.query(collection, projection, extras, null)
        } else {
            val sort = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
            val order = if (limit != null) {
                "$sort LIMIT $limit OFFSET ${offset ?: 0}"
            } else {
                sort
            }
            resolver.query(collection, projection, filter?.selection, filter?.args, order)
        }
    }

    private data class Bucket(
        val id: Long,
        val name: String,
        var count: Int,
        val coverUri: String?,
        val coverId: Long,
    ) {
        fun toAlbum(): Album = Album(
            id = LocalIds.bucket(id),
            ownerId = "local",
            title = name,
            description = "",
            coverPhotoId = LocalIds.photo(coverId),
            createdAt = "",
            photoCount = count,
            coverUrl = coverUri,
            shares = emptyList(),
        )
    }
}

private fun Cursor.indexOf(column: String): Int = getColumnIndex(column)

private fun Cursor.stringOrEmpty(index: Int): String =
    if (index < 0 || isNull(index)) "" else getString(index).orEmpty()

private fun Cursor.intOrZero(index: Int): Int =
    if (index < 0 || isNull(index)) 0 else getInt(index)

private fun Cursor.longOrZero(index: Int): Long =
    if (index < 0 || isNull(index)) 0L else getLong(index)
