package com.example.lyrics

import android.util.Log
import com.example.model.LyricsSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsOnlineSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LyricsSearchService"
    }

    suspend fun searchLyrics(title: String, artist: String): List<LyricsSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LyricsSearchResult>()

        val cleaned = TitleSanitizer.clean(title, artist)
        val query = "${cleaned.cleanArtist} ${cleaned.cleanTitle}".trim()

        // 1. Search LRCLIB
        try {
            val lrclibResults = searchLrclib(cleaned.cleanTitle, cleaned.cleanArtist, query)
            results.addAll(lrclibResults)
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB search error: ${e.message}")
        }

        // 2. Search Netease Cloud Music API (great for Asian / Pop / Japanese songs)
        try {
            val neteaseResults = searchNetease(query)
            results.addAll(neteaseResults)
        } catch (e: Exception) {
            Log.w(TAG, "Netease search error: ${e.message}")
        }

        // If no results with both artist + title, try title only
        if (results.isEmpty() && cleaned.cleanTitle.isNotBlank()) {
            try {
                val lrclibTitleOnly = searchLrclib(cleaned.cleanTitle, "", cleaned.cleanTitle)
                results.addAll(lrclibTitleOnly)
            } catch (e: Exception) {
                Log.w(TAG, "LRCLIB fallback error: ${e.message}")
            }
        }

        results
    }

    private fun searchLrclib(title: String, artist: String, query: String): List<LyricsSearchResult> {
        val list = mutableListOf<LyricsSearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://lrclib.net/api/search?q=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MiniLyrics-Android16/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return list
            val body = response.body?.string() ?: return list
            val jsonArray = JSONArray(body)

            for (i in 0 until minOf(jsonArray.length(), 8)) {
                val obj = jsonArray.getJSONObject(i)
                val trackName = obj.optString("trackName", title)
                val artistName = obj.optString("artistName", artist)
                val albumName = obj.optString("albumName", "")
                val syncedLyrics = obj.optString("syncedLyrics", "")
                val plainLyrics = obj.optString("plainLyrics", "")
                val duration = obj.optInt("duration", 0)
                val id = obj.optString("id", i.toString())

                if (syncedLyrics.isNotBlank() || plainLyrics.isNotBlank()) {
                    list.add(
                        LyricsSearchResult(
                            id = "lrclib_$id",
                            title = trackName,
                            artist = artistName,
                            album = albumName,
                            durationSeconds = duration,
                            isSynced = syncedLyrics.isNotBlank(),
                            source = "LRCLIB",
                            lrcContent = if (syncedLyrics.isNotBlank()) syncedLyrics else plainLyrics,
                            plainContent = plainLyrics,
                            rating = 5.0f - (i * 0.1f),
                            downloads = 350 - (i * 20)
                        )
                    )
                }
            }
        }
        return list
    }

    private fun searchNetease(query: String): List<LyricsSearchResult> {
        val list = mutableListOf<LyricsSearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://music.163.com/api/search/pc?s=$encodedQuery&type=1&offset=0&limit=5"

        val searchRequest = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(searchRequest).execute().use { response ->
            if (!response.isSuccessful) return list
            val body = response.body?.string() ?: return list
            val root = JSONObject(body)
            val result = root.optJSONObject("result") ?: return list
            val songs = result.optJSONArray("songs") ?: return list

            for (i in 0 until songs.length()) {
                val songObj = songs.getJSONObject(i)
                val songId = songObj.optLong("id", 0L)
                val songName = songObj.optString("name", "")
                val duration = songObj.optInt("duration", 0) / 1000

                var artistName = ""
                val artistsArr = songObj.optJSONArray("artists")
                if (artistsArr != null && artistsArr.length() > 0) {
                    artistName = artistsArr.getJSONObject(0).optString("name", "")
                }

                var albumName = ""
                val albumObj = songObj.optJSONObject("album")
                if (albumObj != null) {
                    albumName = albumObj.optString("name", "")
                }

                if (songId > 0) {
                    // Fetch lyric content
                    val lrc = fetchNeteaseLyric(songId)
                    if (lrc.isNotBlank()) {
                        list.add(
                            LyricsSearchResult(
                                id = "netease_$songId",
                                title = songName,
                                artist = artistName,
                                album = albumName,
                                durationSeconds = duration,
                                isSynced = lrc.contains("["),
                                source = "Netease Cloud",
                                lrcContent = lrc,
                                rating = 4.8f,
                                downloads = 520 - (i * 35)
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun fetchNeteaseLyric(songId: Long): String {
        return try {
            val lyricUrl = "https://music.163.com/api/song/lyric?os=pc&id=$songId&lv=-1&kv=-1&tv=-1"
            val request = Request.Builder()
                .url(lyricUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ""
                val body = response.body?.string() ?: return ""
                val root = JSONObject(body)
                val lrcObj = root.optJSONObject("lrc") ?: return ""
                lrcObj.optString("lyric", "")
            }
        } catch (e: Exception) {
            ""
        }
    }
}
