package com.burton.photos.data.local

import com.burton.photos.domain.PhotoQuery
import java.time.LocalDate
import java.time.ZoneOffset

data class SqlFilter(
    val selection: String,
    val args: Array<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SqlFilter) return false
        return selection == other.selection && args.contentEquals(other.args)
    }

    override fun hashCode(): Int = 31 * selection.hashCode() + args.contentHashCode()
}

data class LocalMediaRow(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateTakenMs: Long,
    val dateAddedSec: Long,
    val bucketId: Long,
    val bucketName: String,
    val favorite: Boolean,
) {
    val takenAtMs: Long
        get() = when {
            dateTakenMs > 0L -> dateTakenMs
            dateAddedSec > 0L -> dateAddedSec * 1000L
            else -> 0L
        }
}

object LocalMediaQuery {
    fun filter(query: PhotoQuery, supportsFavorite: Boolean = true): SqlFilter? {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        query.q?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
            clauses += "(${Columns.DISPLAY_NAME} LIKE ? COLLATE NOCASE)"
            args += "%$q%"
        }
        query.folder?.takeIf { it.isNotBlank() }?.let { folder ->
            clauses += "${Columns.BUCKET_ID} = ?"
            args += folder
        }
        if (query.favorite == true && supportsFavorite) {
            clauses += "${Columns.IS_FAVORITE} = 1"
        }
        yearRange(query)?.let { (start, end) ->
            clauses += "((${Columns.DATE_TAKEN} >= ? AND ${Columns.DATE_TAKEN} < ?) OR " +
                "((${Columns.DATE_TAKEN} IS NULL OR ${Columns.DATE_TAKEN} = 0) AND " +
                "${Columns.DATE_ADDED} >= ? AND ${Columns.DATE_ADDED} < ?))"
            args += start.toString()
            args += end.toString()
            args += (start / 1000L).toString()
            args += (end / 1000L).toString()
        }
        if (clauses.isEmpty()) return null
        return SqlFilter(clauses.joinToString(" AND "), args.toTypedArray())
    }

    fun yearRange(query: PhotoQuery): Pair<Long, Long>? {
        val year = query.year?.toIntOrNull() ?: return null
        val month = query.month?.toIntOrNull()
        val startDate = if (month != null && month in 1..12) {
            LocalDate.of(year, month, 1)
        } else {
            LocalDate.of(year, 1, 1)
        }
        val endDate = if (month != null && month in 1..12) {
            startDate.plusMonths(1)
        } else {
            startDate.plusYears(1)
        }
        val start = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val end = endDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return start to end
    }

    object Columns {
        const val DISPLAY_NAME = "display_name"
        const val BUCKET_ID = "bucket_id"
        const val IS_FAVORITE = "is_favorite"
        const val DATE_TAKEN = "datetaken"
        const val DATE_ADDED = "date_added"
    }
}
