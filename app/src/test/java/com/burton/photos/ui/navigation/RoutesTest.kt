package com.burton.photos.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun photoAndAlbumUseDistinctArgumentNames() {
        assertTrue(Routes.PHOTO.contains("{photoId}"))
        assertTrue(Routes.ALBUM.contains("{albumId}"))
        assertTrue(Routes.EDIT.contains("{photoId}"))
        assertFalse(Routes.PHOTO.contains("{id}"))
        assertFalse(Routes.ALBUM.contains("{id}"))
    }
}
