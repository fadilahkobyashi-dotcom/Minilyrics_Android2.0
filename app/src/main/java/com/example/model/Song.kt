package com.example.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val filePath: String = "",
    val albumArtUri: Uri? = null,
    val embeddedArt: ByteArray? = null,
    val trackNumber: Int = 0,
    val dateAdded: Long = 0L,
    val isDemo: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Song
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
