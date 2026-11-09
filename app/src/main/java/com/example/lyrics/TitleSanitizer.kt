package com.example.lyrics

object TitleSanitizer {

    private val junkPatterns = listOf(
        Regex("(?i)\\[.*?(official|audio|video|lyrics|flac|mp3|320kbps|hd|4k|mv|hq).*?\\]"),
        Regex("(?i)\\(.*?(official|audio|video|lyrics|flac|mp3|320kbps|hd|4k|mv|hq|remastered).*?\\)"),
        Regex("(?i)\\b(official\\s+video|official\\s+audio|lyrics?\\s+video|audio|mv)\\b"),
        Regex("(?i)\\b(320kbps|128kbps|flac|mp3|remastered|remaster)\\b"),
        Regex("^[0-9]{1,3}[\\s._-]+"), // Track numbers e.g. "01 - ", "02. "
        Regex("(?i)^youtube[-_\\s]+"),
        Regex("(?i)[_-]?(1080p|720p|480p)"),
        Regex("\\.(mp3|flac|m4a|wav|ogg|aac|wma)$", RegexOption.IGNORE_CASE)
    )

    data class CleanResult(
        val cleanTitle: String,
        val cleanArtist: String
    )

    fun clean(title: String, artist: String, fileName: String = ""): CleanResult {
        var t = title.trim()
        var a = artist.trim()

        // If title is empty or appears to be a raw filename
        if (t.isBlank() && fileName.isNotBlank()) {
            t = fileName
        }

        // Remove extension if present
        t = t.replace(Regex("\\.[a-zA-Z0-9]{2,4}$"), "")

        // Check if artist and title are merged like "Artist - Title" or "Artist _ Title"
        if ((a.isBlank() || a.equals("<unknown>", ignoreCase = true)) && t.contains(" - ")) {
            val parts = t.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                a = parts[0].trim()
                t = parts[1].trim()
            }
        } else if ((a.isBlank() || a.equals("<unknown>", ignoreCase = true)) && t.contains(" _ ")) {
            val parts = t.split(" _ ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                a = parts[0].trim()
                t = parts[1].trim()
            }
        }

        // Apply junk pattern removal
        for (pattern in junkPatterns) {
            t = t.replace(pattern, " ")
            a = a.replace(pattern, " ")
        }

        // Clean extra punctuation and double spaces
        t = t.replace(Regex("[_~|]"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        a = a.replace(Regex("[_~|]"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        // Strip leading/trailing hyphens or brackets
        t = t.trim('-', ' ', '(', ')', '[', ']')
        a = a.trim('-', ' ', '(', ')', '[', ']')

        // Default fallbacks
        if (t.isBlank()) t = if (fileName.isNotBlank()) fileName else "Unknown Title"
        if (a.isBlank() || a.equals("<unknown>", ignoreCase = true)) a = "Unknown Artist"

        return CleanResult(cleanTitle = t, cleanArtist = a)
    }
}
