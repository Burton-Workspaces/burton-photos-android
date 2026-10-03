package com.burton.photos.data.edit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMatricesTest {
    @Test
    fun identityLeavesValuesUnchanged() {
        val pixel = floatArrayOf(10f, 20f, 30f, 40f)
        assertArrayEquals(pixel, apply(ColorMatrices.identity(), pixel), 0.01f)
    }

    @Test
    fun concatAppliesFirstThenSecond() {
        val brightness = ColorMatrices.brightness(1f)
        val contrast = ColorMatrices.contrast(2f)
        val combined = ColorMatrices.concat(brightness, contrast)
        val pixel = floatArrayOf(10f, 10f, 10f, 255f)
        val stepwise = apply(contrast, apply(brightness, pixel))
        assertArrayEquals(stepwise, apply(combined, pixel), 0.05f)
    }

    @Test
    fun defaultEditsAreIdentityColor() {
        val edits = PhotoEdits()
        assertFalse(edits.hasColor)
        assertFalse(edits.hasGeometry)
        assertArrayEquals(ColorMatrices.identity(), edits.colorMatrix(), 0.01f)
    }

    @Test
    fun rotateNormalizes() {
        assertEquals(90, PhotoEdits(rotationDegrees = 450).normalizedRotation())
        assertEquals(270, PhotoEdits().rotateBy(-90).normalizedRotation())
    }

    @Test
    fun monoFilterDesaturates() {
        val gray = apply(PhotoFilter.Mono.matrix(), floatArrayOf(200f, 10f, 10f, 255f))
        assertEquals(gray[0], gray[1], 1f)
        assertEquals(gray[1], gray[2], 1f)
    }

    private fun apply(matrix: FloatArray, rgba: FloatArray): FloatArray {
        val r = rgba[0]
        val g = rgba[1]
        val b = rgba[2]
        val a = rgba[3]
        return floatArrayOf(
            matrix[0] * r + matrix[1] * g + matrix[2] * b + matrix[3] * a + matrix[4],
            matrix[5] * r + matrix[6] * g + matrix[7] * b + matrix[8] * a + matrix[9],
            matrix[10] * r + matrix[11] * g + matrix[12] * b + matrix[13] * a + matrix[14],
            matrix[15] * r + matrix[16] * g + matrix[17] * b + matrix[18] * a + matrix[19],
        )
    }
}

class CropMathTest {
    @Test
    fun squareCropOfWideImageTakesCenter() {
        val rect = CropMath.sourceRect(
            imageWidth = 200,
            imageHeight = 100,
            crop = CropWindow(viewWidth = 100f, viewHeight = 100f),
        )
        assertEquals(50, rect.x)
        assertEquals(0, rect.y)
        assertEquals(100, rect.width)
        assertEquals(100, rect.height)
    }

    @Test
    fun originalAspectAtScaleOneIsFullImage() {
        val rect = CropMath.sourceRect(
            imageWidth = 160,
            imageHeight = 90,
            crop = CropWindow(viewWidth = 320f, viewHeight = 180f),
        )
        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(160, rect.width)
        assertEquals(90, rect.height)
    }

    @Test
    fun zoomTwoTakesCenterQuarter() {
        val rect = CropMath.sourceRect(
            imageWidth = 200,
            imageHeight = 100,
            crop = CropWindow(viewWidth = 100f, viewHeight = 100f, scale = 2f),
        )
        assertEquals(75, rect.x)
        assertEquals(25, rect.y)
        assertEquals(50, rect.width)
        assertEquals(50, rect.height)
    }

    @Test
    fun panIsClampedToCoverCrop() {
        val window = CropWindow(viewWidth = 100f, viewHeight = 100f, panX = 10_000f, panY = -10_000f)
        val (x, y) = CropMath.clampPan(200f, 100f, window)
        assertEquals(50f, x, 0.01f)
        assertEquals(0f, y, 0.01f)
        assertTrue(x <= 50f)
    }

    @Test
    fun cropSizeFitsInsideBounds() {
        val (w, h) = CropMath.cropSize(200f, 100f, 1f)
        assertEquals(100f, w, 0.01f)
        assertEquals(100f, h, 0.01f)
        val wide = CropMath.cropSize(200f, 100f, 16f / 9f)
        assertEquals(100f * 16f / 9f, wide.first, 0.01f)
        assertEquals(100f, wide.second, 0.01f)
    }
}
