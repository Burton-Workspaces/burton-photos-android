package com.burton.photos.data.local

object LocalIds {
    private const val PHOTO = "local-"
    private const val BUCKET = "bucket-"

    fun photo(mediaId: Long): String = PHOTO + mediaId

    fun isPhoto(id: String): Boolean {
        val rest = id.removePrefix(PHOTO)
        return rest != id && rest.isNotEmpty() && rest.all { it.isDigit() }
    }

    fun photoMediaId(id: String): Long {
        require(isPhoto(id)) { "Not a local photo id: $id" }
        return id.removePrefix(PHOTO).toLong()
    }

    fun bucket(bucketId: Long): String = BUCKET + bucketId

    fun isBucket(id: String): Boolean {
        val rest = id.removePrefix(BUCKET)
        return rest != id && rest.isNotEmpty() && rest.all { it.isDigit() }
    }

    fun bucketId(id: String): Long {
        require(isBucket(id)) { "Not a local album id: $id" }
        return id.removePrefix(BUCKET).toLong()
    }
}
