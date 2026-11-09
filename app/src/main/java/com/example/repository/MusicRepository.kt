package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import com.example.data.LyricsAssociation
import com.example.data.MiniLyricsDatabase
import com.example.lyrics.LrcParser
import com.example.lyrics.LyricsOnlineSearchService
import com.example.lyrics.TitleSanitizer
import com.example.model.LyricsData
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    private val db = MiniLyricsDatabase.getDatabase(context)
    private val dao = db.dao()
    private val onlineSearch = LyricsOnlineSearchService()

    // In-memory artwork cache
    private val artworkCache = object : LruCache<Long, ByteArray>(50) {}

    companion object {
        private const val TAG = "MusicRepository"
    }

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackCol = it.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val dateCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val rawTitle = it.getString(titleCol) ?: "Unknown"
                    val rawArtist = it.getString(artistCol) ?: "<unknown>"
                    val rawAlbum = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durCol)
                    val albumId = it.getLong(albumIdCol)
                    val path = it.getString(dataCol) ?: ""
                    val trackNum = if (trackCol >= 0) it.getInt(trackCol) else 0
                    val dateAdded = if (dateCol >= 0) it.getLong(dateCol) else 0L

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    // Check if user has saved custom title / artist for this song file
                    val association = dao.getLyricsAssociation(path)
                    val title = association?.customTitle ?: rawTitle
                    val artist = association?.customArtist ?: rawArtist
                    val album = association?.customAlbum ?: rawAlbum

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            filePath = path,
                            albumArtUri = albumArtUri,
                            trackNumber = trackNum,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}")
        }

        // If local library is empty (e.g. freshly installed emulator), provide bundled demo songs with synced lyrics
        if (songsList.isEmpty()) {
            songsList.addAll(getBundledDemoSongs())
        }

        songsList
    }

    suspend fun getAlbumArt(song: Song): ByteArray? = withContext(Dispatchers.IO) {
        if (artworkCache.get(song.id) != null) {
            return@withContext artworkCache.get(song.id)
        }

        // Try extracting embedded album art via MediaMetadataRetriever
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.contentUri)
            val picture = retriever.embeddedPicture
            retriever.release()
            if (picture != null) {
                artworkCache.put(song.id, picture)
                return@withContext picture
            }
        } catch (e: Exception) {
            // Ignored
        }

        null
    }

    suspend fun loadLyricsForSong(song: Song, autoSearch: Boolean = true): LyricsData = withContext(Dispatchers.IO) {
        val filePath = song.filePath

        // 1. Check database for existing association
        val association = dao.getLyricsAssociation(filePath)
        if (association != null && association.lyricsContent.isNotBlank()) {
            val parsed = LrcParser.parse(association.lyricsContent, association.source)
            return@withContext LrcParser.applyOffset(parsed, association.offsetMs)
        }

        // 2. Check for local .lrc file in same folder
        if (filePath.isNotBlank()) {
            val file = File(filePath)
            val lrcFile1 = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
            val lrcFile2 = File("$filePath.lrc")

            val targetFile = when {
                lrcFile1.exists() -> lrcFile1
                lrcFile2.exists() -> lrcFile2
                else -> null
            }

            if (targetFile != null && targetFile.canRead()) {
                try {
                    val lrcText = targetFile.readText(Charsets.UTF_8)
                    if (lrcText.isNotBlank()) {
                        val parsed = LrcParser.parse(lrcText, "Local File (${targetFile.name})")
                        // Cache into DB
                        dao.saveLyricsAssociation(
                            LyricsAssociation(
                                songFile = filePath,
                                lyricsContent = lrcText,
                                source = "Local LRC"
                            )
                        )
                        return@withContext parsed
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading local LRC: ${e.message}")
                }
            }
        }

        // 3. For bundled demo songs, return rich demo synchronized Japanese & English lyrics
        if (song.isDemo) {
            val demoLrc = getDemoLyrics(song.id)
            if (demoLrc.isNotBlank()) {
                return@withContext LrcParser.parse(demoLrc, "Demo Synchronized")
            }
        }

        // 4. Auto-search online if enabled
        if (autoSearch) {
            try {
                val cleaned = TitleSanitizer.clean(song.title, song.artist, File(filePath).name)
                val results = onlineSearch.searchLyrics(cleaned.cleanTitle, cleaned.cleanArtist)
                val bestMatch = results.firstOrNull { it.isSynced } ?: results.firstOrNull()

                if (bestMatch != null && bestMatch.lrcContent.isNotBlank()) {
                    val parsed = LrcParser.parse(bestMatch.lrcContent, bestMatch.source)
                    dao.saveLyricsAssociation(
                        LyricsAssociation(
                            songFile = filePath,
                            lyricsContent = bestMatch.lrcContent,
                            source = bestMatch.source
                        )
                    )
                    return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto online lyrics search failed: ${e.message}")
            }
        }

        LyricsData(
            title = song.title,
            artist = song.artist,
            album = song.album,
            source = "None",
            lines = emptyList()
        )
    }

    suspend fun saveLyricsForSong(song: Song, lrcContent: String, source: String, offsetMs: Long = 0L) = withContext(Dispatchers.IO) {
        dao.saveLyricsAssociation(
            LyricsAssociation(
                songFile = song.filePath,
                lyricsContent = lrcContent,
                offsetMs = offsetMs,
                source = source
            )
        )
    }

    suspend fun updateTrackInfo(song: Song, newTitle: String, newArtist: String, newAlbum: String) = withContext(Dispatchers.IO) {
        val existing = dao.getLyricsAssociation(song.filePath)
        if (existing != null) {
            dao.saveLyricsAssociation(
                existing.copy(
                    customTitle = newTitle,
                    customArtist = newArtist,
                    customAlbum = newAlbum,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            dao.saveLyricsAssociation(
                LyricsAssociation(
                    songFile = song.filePath,
                    lyricsContent = "",
                    customTitle = newTitle,
                    customArtist = newArtist,
                    customAlbum = newAlbum
                )
            )
        }
    }

    suspend fun updateLyricsOffset(song: Song, newOffsetMs: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getLyricsAssociation(song.filePath)
        if (existing != null) {
            dao.saveLyricsAssociation(existing.copy(offsetMs = newOffsetMs))
        }
    }

    // Favorites
    fun getFavoriteSongIds() = dao.getFavoriteSongIds()
    suspend fun toggleFavorite(songId: Long) = withContext(Dispatchers.IO) {
        if (dao.isFavorite(songId)) {
            dao.removeFavorite(songId)
        } else {
            dao.addFavorite(com.example.data.FavoriteSong(songId))
        }
    }

    fun getRecentSongIds() = dao.getRecentSongIds()

    private fun getBundledDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = 999001L,
                title = "Hikari (光) - Simple and Clean",
                artist = "Utada Hikaru (宇多田ヒカル)",
                album = "Deep River",
                durationMs = 75000L,
                contentUri = Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.demo_hikari}"),
                filePath = "/demo/hikari.mp3",
                isDemo = true
            ),
            Song(
                id = 999002L,
                title = "Sakura Mitsutsuki (サクラミツツキ)",
                artist = "SPYAIR",
                album = "Million",
                durationMs = 60000L,
                contentUri = Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.demo_sakura}"),
                filePath = "/demo/sakura.mp3",
                isDemo = true
            ),
            Song(
                id = 999003L,
                title = "Neon Dreams (Acoustic Version)",
                artist = "MiniLyrics Ensemble",
                album = "MiniLyrics Originals",
                durationMs = 60000L,
                contentUri = Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.demo_neon}"),
                filePath = "/demo/neon_dreams.mp3",
                isDemo = true
            )
        )
    }

    private fun getDemoLyrics(songId: Long): String {
        return when (songId) {
            999001L -> """
                [ti:Hikari (光)]
                [ar:Utada Hikaru]
                [al:Deep River]
                [00:00.00]MiniLyrics Synchronized Display
                [00:04.50]どんな時だって たった一人で
                [00:09.80]運命忘れて 生きてきたけれど
                [00:15.20]突然の光の中 目覚める
                [00:20.60]真夜中に 静かな雨が降る
                [00:26.00]君の声が聴こえる
                [00:31.50]胸の奥が熱くなってゆく
                [00:37.00]果てしない空を見上げて
                [00:43.00]約束の場所へ歩き出す
                [00:49.00]光を求めて 翼広げて
                [00:55.00]永遠の絆を 信じているから
                [01:02.00]サヨナラは言わないで
                [01:08.00]未来へ繋がる 明日へ
            """.trimIndent()

            999002L -> """
                [ti:サクラミツツキ]
                [ar:SPYAIR]
                [al:Million]
                [00:00.00]MiniLyrics Synchronized Display
                [00:05.00]春の夜風に 桜が舞い散る
                [00:11.20]満月の下で 君を想う
                [00:17.50]忘れないよ あの日の笑顔
                [00:24.00]涙さえも 強さに変えて
                [00:30.80]僕らは今 走り出す
                [00:37.50]夜明けの向こう 輝く世界へ
            """.trimIndent()

            else -> """
                [ti:Neon Dreams]
                [ar:MiniLyrics Ensemble]
                [al:MiniLyrics Originals]
                [00:00.00]MiniLyrics Audio Player
                [00:04.00]The rhythm moves us dancing in the light
                [00:08.50]A melody that whispers through the night
                [00:13.20]Feel the sound vibrating deep inside
                [00:18.00]With every beat we let our spirits glide
                [00:23.50]Synchronized words shining bright
                [00:28.00]Underneath the neon midnight sky
            """.trimIndent()
        }
    }
}
