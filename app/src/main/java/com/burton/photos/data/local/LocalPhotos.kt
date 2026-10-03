package com.burton.photos.data.local

import com.burton.photos.domain.Photo
import java.time.Instant

fun LocalMediaRow.toPhoto(): Photo {
    val taken = takenAtMs.takeIf { it > 0L }?.let { Instant.ofEpochMilli(it).toString() }
    val title = displayName.substringBeforeLast('.').ifBlank { displayName }
    return Photo(
        id = LocalIds.photo(id),
        ownerId = "local",
        groupId = null,
        filename = displayName,
        folder = bucketName,
        title = title,
        description = "",
        mime = mime.ifBlank { "image/*" },
        width = width,
        height = height,
        size = size,
        takenAt = taken,
        cameraMake = null,
        cameraModel = null,
        lens = null,
        iso = null,
        aperture = null,
        shutter = null,
        focalLength = null,
        lat = null,
        lng = null,
        country = null,
        city = null,
        favorite = favorite,
        private = false,
        archived = false,
        thumbUrl = uri,
        thumbSmUrl = uri,
        thumbXlUrl = uri,
        originalUrl = uri,
    )
}
