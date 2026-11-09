package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_association")
data class LyricsAssociation(
    @PrimaryKey
    val songFile: String,
    val lyricsContent: String,
    val offsetMs: Long = 0L,
    val source: String = "Online",
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteSong(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_playlists")
data class CustomPlaylist(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0
)
