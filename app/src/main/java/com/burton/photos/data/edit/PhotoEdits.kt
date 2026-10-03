package com.burton.photos.data.edit

data class PhotoEdits(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.None,
) {
    val hasGeometry: Boolean
        get() = normalizedRotation() != 0 || flipHorizontal || flipVertical

    val hasColor: Boolean
        get() = brightness != 0f || contrast != 0f || saturation != 0f ||
            warmth != 0f || filter != PhotoFilter.None

    fun normalizedRotation(): Int = ((rotationDegrees % 360) + 360) % 360

    fun rotateBy(delta: Int): PhotoEdits = copy(rotationDegrees = normalizedRotation() + delta)

    fun colorMatrix(): FloatArray {
        var matrix = ColorMatrices.identity()
        matrix = ColorMatrices.concat(matrix, ColorMatrices.saturation(1f + saturation))
        matrix = ColorMatrices.concat(matrix, ColorMatrices.warmth(warmth))
        matrix = ColorMatrices.concat(matrix, ColorMatrices.contrast(1f + contrast))
        matrix = ColorMatrices.concat(matrix, ColorMatrices.brightness(brightness))
        matrix = ColorMatrices.concat(matrix, filter.matrix())
        return matrix
    }
}

enum class PhotoFilter(val label: String) {
    None("Original"),
    Mono("Mono"),
    Chrome("Chrome"),
    Fade("Fade"),
    Cool("Cool"),
    Warm("Warm"),
    ;

    fun matrix(): FloatArray = when (this) {
        None -> ColorMatrices.identity()
        Mono -> ColorMatrices.saturation(0f)
        Chrome -> ColorMatrices.concat(
            ColorMatrices.saturation(0.85f),
            ColorMatrices.contrast(1.18f),
        )
        Fade -> ColorMatrices.concat(
            ColorMatrices.contrast(0.82f),
            ColorMatrices.brightness(0.08f),
        )
        Cool -> ColorMatrices.concat(
            ColorMatrices.warmth(-0.35f),
            ColorMatrices.contrast(1.06f),
        )
        Warm -> ColorMatrices.concat(
            ColorMatrices.warmth(0.4f),
            ColorMatrices.saturation(1.08f),
        )
    }
}

enum class CropAspect(val label: String, val ratio: Float?) {
    Original("Original", null),
    Free("Free", null),
    Square("1:1", 1f),
    FourThree("4:3", 4f / 3f),
    ThreeFour("3:4", 3f / 4f),
    SixteenNine("16:9", 16f / 9f),
}

data class CropWindow(
    val viewWidth: Float,
    val viewHeight: Float,
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
)

data class PixelRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

object ColorMatrices {
    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    fun brightness(amount: Float): FloatArray {
        val t = amount * 127.5f
        return floatArrayOf(
            1f, 0f, 0f, 0f, t,
            0f, 1f, 0f, 0f, t,
            0f, 0f, 1f, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun contrast(amount: Float): FloatArray {
        val t = 127.5f * (1f - amount)
        return floatArrayOf(
            amount, 0f, 0f, 0f, t,
            0f, amount, 0f, 0f, t,
            0f, 0f, amount, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun saturation(amount: Float): FloatArray {
        val inv = 1f - amount
        val r = 0.213f * inv
        val g = 0.715f * inv
        val b = 0.072f * inv
        return floatArrayOf(
            r + amount, g, b, 0f, 0f,
            r, g + amount, b, 0f, 0f,
            r, g, b + amount, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun warmth(amount: Float): FloatArray {
        val t = amount * 50f
        return floatArrayOf(
            1f, 0f, 0f, 0f, t,
            0f, 1f, 0f, 0f, t * 0.2f,
            0f, 0f, 1f, 0f, -t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** Matrix that applies [first], then [second]. */
    fun concat(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0..3) {
            val o = row * 5
            for (col in 0..4) {
                var value =
                    second[o] * first[col] +
                        second[o + 1] * first[5 + col] +
                        second[o + 2] * first[10 + col] +
                        second[o + 3] * first[15 + col]
                if (col == 4) value += second[o + 4]
                out[o + col] = value
            }
        }
        return out
    }
}

object CropMath {
    fun sourceRect(imageWidth: Int, imageHeight: Int, crop: CropWindow): PixelRect {
        val cropW = crop.viewWidth.coerceAtLeast(1f)
        val cropH = crop.viewHeight.coerceAtLeast(1f)
        val imgW = imageWidth.toFloat().coerceAtLeast(1f)
        val imgH = imageHeight.toFloat().coerceAtLeast(1f)
        val (panX, panY) = clampPan(imgW, imgH, crop)
        val scale = coverScale(imgW, imgH, cropW, cropH) * crop.scale.coerceAtLeast(1f)
        val drawnW = imgW * scale
        val drawnH = imgH * scale
        val left = (cropW - drawnW) / 2f + panX
        val top = (cropH - drawnH) / 2f + panY
        val srcLeft = ((0f - left) / scale).coerceIn(0f, imgW)
        val srcTop = ((0f - top) / scale).coerceIn(0f, imgH)
        val srcRight = ((cropW - left) / scale).coerceIn(0f, imgW)
        val srcBottom = ((cropH - top) / scale).coerceIn(0f, imgH)
        val x = srcLeft.toInt().coerceIn(0, imageWidth - 1)
        val y = srcTop.toInt().coerceIn(0, imageHeight - 1)
        val width = (srcRight - srcLeft).toInt().coerceAtLeast(1).coerceAtMost(imageWidth - x)
        val height = (srcBottom - srcTop).toInt().coerceAtLeast(1).coerceAtMost(imageHeight - y)
        return PixelRect(x, y, width, height)
    }

    fun clampPan(imageWidth: Float, imageHeight: Float, crop: CropWindow): Pair<Float, Float> {
        val cropW = crop.viewWidth.coerceAtLeast(1f)
        val cropH = crop.viewHeight.coerceAtLeast(1f)
        val scale = coverScale(imageWidth, imageHeight, cropW, cropH) * crop.scale.coerceAtLeast(1f)
        val maxX = ((imageWidth * scale - cropW) / 2f).coerceAtLeast(0f)
        val maxY = ((imageHeight * scale - cropH) / 2f).coerceAtLeast(0f)
        return crop.panX.coerceIn(-maxX, maxX) to crop.panY.coerceIn(-maxY, maxY)
    }

    fun coverScale(imageWidth: Float, imageHeight: Float, cropWidth: Float, cropHeight: Float): Float {
        val imgW = imageWidth.coerceAtLeast(1f)
        val imgH = imageHeight.coerceAtLeast(1f)
        return maxOf(cropWidth / imgW, cropHeight / imgH)
    }

    fun cropSize(maxWidth: Float, maxHeight: Float, aspect: Float): Pair<Float, Float> {
        val ratio = aspect.coerceAtLeast(0.05f)
        return if (maxWidth / maxHeight.coerceAtLeast(1f) > ratio) {
            maxHeight * ratio to maxHeight
        } else {
            maxWidth to maxWidth / ratio
        }
    }
}
