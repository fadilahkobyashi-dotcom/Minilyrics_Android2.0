package com.example.model

data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val romaji: String = "",
    val translation: String = ""
)

data class LyricsData(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val offsetMs: Long = 0L,
    val lines: List<LyricsLine> = emptyList(),
    val rawLrc: String = "",
    val source: String = "Local"
) {
    val isSynced: Boolean
        get() = lines.any { it.timeMs > 0 }
}

data class LyricsSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int = 0,
    val isSynced: Boolean = true,
    val source: String = "LRCLIB",
    val lrcContent: String = "",
    val plainContent: String = "",
    val rating: Float = 5.0f,
    val downloads: Int = 120
)
