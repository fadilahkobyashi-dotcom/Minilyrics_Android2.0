package com.example

import com.example.lyrics.LrcParser
import com.example.lyrics.TitleSanitizer
import com.example.lyrics.romanization.RomajiConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLrcParserSyncLyrics() {
        val lrc = """
            [ti:Test Song]
            [ar:MiniLyrics Artist]
            [al:Demo Album]
            [00:01.50]First line of lyrics
            [00:04.20]Second line of lyrics
        """.trimIndent()

        val parsed = LrcParser.parse(lrc, "Test")
        assertEquals("Test Song", parsed.title)
        assertEquals("MiniLyrics Artist", parsed.artist)
        assertEquals("Demo Album", parsed.album)
        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(1500L, parsed.lines[0].timeMs)
        assertEquals("First line of lyrics", parsed.lines[0].text)
        assertEquals(4200L, parsed.lines[1].timeMs)
        assertEquals("Second line of lyrics", parsed.lines[1].text)
    }

    @Test
    fun testTitleSanitizer() {
        val result = TitleSanitizer.clean("01. Artist - Neon City (Official Audio) [FLAC] [1080p].mp3", "")
        assertEquals("Neon City", result.cleanTitle)
        assertEquals("Artist", result.cleanArtist)
    }

    @Test
    fun testRomajiConverter() {
        val hiragana = "さくら"
        val romaji = RomajiConverter.convertLine(hiragana)
        assertEquals("sakura", romaji)
    }
}
