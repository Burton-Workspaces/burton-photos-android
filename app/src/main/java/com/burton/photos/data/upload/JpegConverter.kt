package com.burton.photos.data.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class PreparedUpload(
    val fileName: String,
    val bytes: ByteArray,
    val mime: String,
)

@Singleton
class JpegConverter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun prepare(uri: Uri): PreparedUpload {
        val name = displayName(uri)
        val mime = context.contentResolver.getType(uri).orEmpty()
        val needsJpeg = needsConvert(name, mime)
        if (!needsJpeg && isEngineFormat(mime)
        ) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Could not read $name")
            return PreparedUpload(name, bytes, mime)
        }
        val bitmap = decode(uri) ?: error("Could not decode $name")
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        val jpegName = name.substringBeforeLast('.') + ".jpg"
        return PreparedUpload(jpegName, out.toByteArray(), "image/jpeg")
    }

    private fun decode(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    private fun displayName(uri: Uri): String {
        val fromCursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        if (!fromCursor.isNullOrBlank()) return fromCursor
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
        return if (ext.isNullOrBlank()) "upload.jpg" else "upload.$ext"
    }

    companion object {
        fun needsConvert(fileName: String, mime: String): Boolean {
            val lower = fileName.lowercase()
            val type = mime.lowercase()
            return type.contains("heic") || type.contains("heif") ||
                lower.endsWith(".heic") || lower.endsWith(".heif") ||
                type.isBlank() || type == "application/octet-stream"
        }

        fun isEngineFormat(mime: String): Boolean {
            val type = mime.lowercase()
            return type == "image/jpeg" || type == "image/jpg" || type == "image/png" ||
                type == "image/webp" || type == "image/gif" || type == "image/tiff" ||
                type == "image/tif"
        }
    }
}
