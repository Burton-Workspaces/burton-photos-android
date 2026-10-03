package com.burton.photos.data.local

import com.burton.photos.data.api.SessionStore
import com.burton.photos.domain.PhotoQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalIdsTest {
    @Test
    fun photoRoundTrip() {
        assertEquals("local-42", LocalIds.photo(42))
        assertTrue(LocalIds.isPhoto("local-42"))
        assertEquals(42L, LocalIds.photoMediaId("local-42"))
        assertFalse(LocalIds.isPhoto("p1"))
        assertFalse(LocalIds.isPhoto("local-"))
        assertFalse(LocalIds.isPhoto("bucket-9"))
    }

    @Test
    fun bucketRoundTrip() {
        assertEquals("bucket-7", LocalIds.bucket(7))
        assertTrue(LocalIds.isBucket("bucket-7"))
        assertEquals(7L, LocalIds.bucketId("bucket-7"))
        assertFalse(LocalIds.isBucket("local-7"))
    }
}

class LocalMediaQueryTest {
    @Test
    fun emptyQueryHasNoFilter() {
        assertNull(LocalMediaQuery.filter(PhotoQuery()))
    }

    @Test
    fun searchAndFolder() {
        val filter = LocalMediaQuery.filter(PhotoQuery(q = "Lake", folder = "12"))!!
        assertTrue(filter.selection.contains("display_name"))
        assertTrue(filter.selection.contains("bucket_id"))
        assertEquals(listOf("%Lake%", "12"), filter.args.toList())
    }

    @Test
    fun favoriteRequiresSupport() {
        val enabled = LocalMediaQuery.filter(PhotoQuery(favorite = true), supportsFavorite = true)!!
        assertTrue(enabled.selection.contains("is_favorite"))
        assertNull(LocalMediaQuery.filter(PhotoQuery(favorite = true), supportsFavorite = false))
    }

    @Test
    fun yearMonthRange() {
        val range = LocalMediaQuery.yearRange(PhotoQuery(year = "2026", month = "03"))!!
        val filter = LocalMediaQuery.filter(PhotoQuery(year = "2026", month = "03"))!!
        assertTrue(filter.selection.contains("datetaken"))
        assertEquals(range.first.toString(), filter.args[0])
        assertEquals(range.second.toString(), filter.args[1])
        assertEquals((range.first / 1000).toString(), filter.args[2])
        assertEquals((range.second / 1000).toString(), filter.args[3])
        assertTrue(range.second > range.first)
    }
}

class LocalPhotoMapperTest {
    @Test
    fun mapsRowToPhoto() {
        val photo = LocalMediaRow(
            id = 9,
            uri = "content://media/external/images/media/9",
            displayName = "IMG_0001.jpg",
            mime = "image/jpeg",
            width = 100,
            height = 80,
            size = 1234,
            dateTakenMs = 1_704_067_200_000L,
            dateAddedSec = 0,
            bucketId = 3,
            bucketName = "Camera",
            favorite = true,
        ).toPhoto()
        assertEquals("local-9", photo.id)
        assertEquals("IMG_0001", photo.title)
        assertEquals("Camera", photo.folder)
        assertTrue(photo.favorite)
        assertEquals("content://media/external/images/media/9", photo.originalUrl)
        assertEquals("content://media/external/images/media/9", photo.thumbSmUrl)
        assertEquals("2024-01-01T00:00:00Z", photo.takenAt)
    }
}

class SessionStoreAbsoluteTest {
    @Test
    fun passesThroughLocalUris() {
        val store = SessionStore()
        store.baseUrl = "https://photos.example"
        assertEquals(
            "content://media/external/images/media/1",
            store.absolute("content://media/external/images/media/1"),
        )
        assertEquals("https://cdn.example/a.jpg", store.absolute("https://cdn.example/a.jpg"))
        assertEquals("https://photos.example/media/thumb/p1/sm", store.absolute("/media/thumb/p1/sm"))
        assertTrue(SessionStore.hasScheme("file:///tmp/a.jpg"))
        assertFalse(SessionStore.hasScheme("/media/a"))
    }
}
