package com.burton.photos.data.edit

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditedPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    suspend fun load(url: String): Bitmap = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(MAX_EDGE)
            .precision(Precision.INEXACT)
            .build()
        val result = imageLoader.execute(request) as? SuccessResult
            ?: error("Could not load photo")
        result.drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
            ?: error("Could not load photo")
    }

    fun oriented(source: Bitmap, edits: PhotoEdits): Bitmap {
        if (!edits.hasGeometry) return source
        val matrix = Matrix()
        val cx = source.width / 2f
        val cy = source.height / 2f
        val rotation = edits.normalizedRotation().toFloat()
        if (rotation != 0f) matrix.postRotate(rotation, cx, cy)
        if (edits.flipHorizontal) matrix.postScale(-1f, 1f, cx, cy)
        if (edits.flipVertical) matrix.postScale(1f, -1f, cx, cy)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun export(source: Bitmap, edits: PhotoEdits, crop: CropWindow): Bitmap {
        val oriented = oriented(source, edits)
        val rect = CropMath.sourceRect(oriented.width, oriented.height, crop)
        val cropped = Bitmap.createBitmap(oriented, rect.x, rect.y, rect.width, rect.height)
        val out = if (!edits.hasColor) {
            if (cropped.config == Bitmap.Config.ARGB_8888) cropped
            else cropped.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            val painted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(edits.colorMatrix()))
            }
            Canvas(painted).drawBitmap(cropped, 0f, 0f, paint)
            if (cropped !== oriented) cropped.recycle()
            painted
        }
        if (oriented !== source && oriented !== out && oriented !== cropped) {
            oriented.recycle()
        }
        return out
    }

    suspend fun saveJpeg(bitmap: Bitmap, fileName: String): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Burton Photos")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not save photo")
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    "Could not write photo"
                }
            } ?: error("Could not write photo")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            ContentUris.parseId(uri)
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    fun editedFileName(original: String): String {
        val base = original.substringBeforeLast('.').ifBlank { "photo" }
        return "${base}_edit.jpg"
    }

    companion object {
        private const val MAX_EDGE = 4096
    }
}
