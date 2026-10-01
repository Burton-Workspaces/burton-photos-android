package com.burton.photos.data.parse

import com.burton.photos.data.upload.JpegConverter
import com.burton.photos.domain.PhotoQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosJsonTest {
    @Test
    fun authConfig() {
        val config = PhotosJson.authConfig(
            """{"mode":"one","register":false,"autoLogin":true,"loginHint":"one"}""",
        )
        assertEquals("one", config.mode)
        assertFalse(config.register)
        assertTrue(config.autoLogin)
        assertEquals("one", config.loginHint)
    }

    @Test
    fun sessionIncludesTokenForNativeClient() {
        val (user, token, mode) = PhotosJson.session(
            """{"user":{"id":"u1","email":"leo@burton.local","name":"Leo","role":"admin"},"token":"abc","expiresAt":"2030-01-01T00:00:00.000Z","mode":"accounts"}""",
        )
        assertEquals("u1", user.id)
        assertEquals("Leo", user.name)
        assertEquals("abc", token)
        assertEquals("accounts", mode)
    }

    @Test
    fun photoPage() {
        val json = """
            {"photos":[{"id":"p1","ownerId":"u1","groupId":null,"filename":"a.jpg","folder":"uploads/2026","title":"A","description":"","mime":"image/jpeg","width":100,"height":80,"size":1234,"takenAt":"2026-01-01T00:00:00.000Z","cameraMake":"Fuji","cameraModel":"X100","lens":null,"iso":200,"aperture":2.8,"shutter":"1/250","focalLength":23,"lat":null,"lng":null,"country":null,"city":"Nashville","favorite":true,"private":false,"archived":false,"thumbUrl":"/media/thumb/p1/md","thumbSmUrl":"/media/thumb/p1/sm","thumbXlUrl":"/media/thumb/p1/xl","originalUrl":"/media/original/p1"}],"total":1,"limit":120,"offset":0}
        """.trimIndent()
        val page = PhotosJson.photoPage(json)
        assertEquals(1, page.total)
        assertEquals("p1", page.photos[0].id)
        assertEquals("Nashville", page.photos[0].city)
        assertTrue(page.photos[0].favorite)
        assertEquals("/media/thumb/p1/md", page.photos[0].thumbUrl)
    }

    @Test
    fun albums() {
        val json = """{"albums":[{"id":"a1","ownerId":"u1","title":"Trip","description":"","coverPhotoId":null,"createdAt":"2026-01-01T00:00:00.000Z","photoCount":3,"coverUrl":"/media/thumb/p1/md","shares":[{"id":"g1","name":"Family"}]}]}"""
        val albums = PhotosJson.albums(json)
        assertEquals("Trip", albums[0].title)
        assertEquals(3, albums[0].photoCount)
        assertEquals("Family", albums[0].shares[0].name)
    }

    @Test
    fun errorMessage() {
        assertEquals("Unauthorized", PhotosJson.errorMessage("""{"error":"Unauthorized"}""", "fallback"))
        assertEquals("fallback", PhotosJson.errorMessage("not-json", "fallback"))
    }
}

class PhotoQueryTest {
    @Test
    fun queryPairsSkipBlanks() {
        val pairs = PhotoQuery(q = "lake", favorite = true, year = "2026").queryPairs()
        assertEquals(
            listOf("q" to "lake", "year" to "2026", "favorite" to "true"),
            pairs,
        )
    }

    @Test
    fun archivedFlag() {
        assertEquals(listOf("archived" to "true"), PhotoQuery(archived = true).queryPairs())
        assertTrue(PhotoQuery().queryPairs().isEmpty())
    }
}

class JpegConverterTest {
    @Test
    fun heicNeedsConvert() {
        assertTrue(JpegConverter.needsConvert("IMG_0001.HEIC", "image/heic"))
        assertTrue(JpegConverter.needsConvert("shot.heif", ""))
        assertFalse(JpegConverter.needsConvert("shot.jpg", "image/jpeg"))
        assertTrue(JpegConverter.isEngineFormat("image/png"))
        assertFalse(JpegConverter.isEngineFormat("image/heic"))
    }
}
