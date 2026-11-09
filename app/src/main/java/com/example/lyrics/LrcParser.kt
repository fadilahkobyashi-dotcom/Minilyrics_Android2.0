package com.example.lyrics

import com.example.lyrics.romanization.RomajiConverter
import com.example.model.LyricsData
import com.example.model.LyricsLine
import java.util.regex.Pattern

object LrcParser {

    private val timeTagPattern = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val metaTagPattern = Pattern.compile("\\[([a-zA-Z]+):([^\\]]*)\\]")

    fun parse(lrcText: String, source: String = "Online"): LyricsData {
        if (lrcText.isBlank()) {
            return LyricsData(source = source)
        }

        var title = ""
        var artist = ""
        var album = ""
        var offsetMs = 0L

        val parsedLines = mutableListOf<LyricsLine>()
        val rawLines = lrcText.lines()

        var hasTimeTags = false

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Parse metadata tags
            val metaMatcher = metaTagPattern.matcher(trimmed)
            if (metaMatcher.find() && !trimmed.matches(Regex("^\\[\\d+.*"))) {
                val tag = metaMatcher.group(1)?.lowercase()
                val value = metaMatcher.group(2)?.trim() ?: ""
                when (tag) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                continue
            }

            // Extract all time tags in this line
            val timeMatcher = timeTagPattern.matcher(trimmed)
            val lineTimes = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (timeMatcher.find()) {
                hasTimeTags = true
                val minutes = timeMatcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = timeMatcher.group(2)?.toLongOrNull() ?: 0L
                val fractionStr = timeMatcher.group(3) ?: "0"
                val fractionMs = when (fractionStr.length) {
                    1 -> fractionStr.toLong() * 100
                    2 -> fractionStr.toLong() * 10
                    else -> fractionStr.take(3).toLong()
                }

                val timeMs = (minutes * 60 * 1000) + (seconds * 1000) + fractionMs
                lineTimes.add(timeMs)
                lastMatchEnd = timeMatcher.end()
            }

            val lyricText = if (lastMatchEnd > 0 && lastMatchEnd <= trimmed.length) {
                trimmed.substring(lastMatchEnd).trim()
            } else if (!hasTimeTags) {
                trimmed
            } else {
                ""
            }

            // Generate Romaji transliteration
            val romajiText = if (lyricText.isNotEmpty()) {
                RomajiConverter.convertLine(lyricText)
            } else ""

            if (lineTimes.isNotEmpty()) {
                for (t in lineTimes) {
                    parsedLines.add(
                        LyricsLine(
                            timeMs = maxOf(0L, t + offsetMs),
                            text = lyricText,
                            romaji = romajiText
                        )
                    )
                }
            } else if (!hasTimeTags && lyricText.isNotEmpty()) {
                // Untimed line fallback
                parsedLines.add(
                    LyricsLine(
                        timeMs = 0L,
                        text = lyricText,
                        romaji = romajiText
                    )
                )
            }
        }

        // Sort lines chronologically
        val sortedLines = if (hasTimeTags) {
            parsedLines.sortedBy { it.timeMs }
        } else {
            parsedLines
        }

        return LyricsData(
            title = title,
            artist = artist,
            album = album,
            offsetMs = offsetMs,
            lines = sortedLines,
            rawLrc = lrcText,
            source = source
        )
    }

    /**
     * Shifts all timestamps by a relative delta (e.g. +500ms or -500ms)
     */
    fun applyOffset(data: LyricsData, deltaMs: Long): LyricsData {
        val newLines = data.lines.map {
            it.copy(timeMs = maxOf(0L, it.timeMs + deltaMs))
        }
        return data.copy(
            offsetMs = data.offsetMs + deltaMs,
            lines = newLines
        )
    }
}
