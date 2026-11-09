package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MiniLyricsDao {

    @Query("SELECT * FROM lyrics_association WHERE songFile = :songFile LIMIT 1")
    suspend fun getLyricsAssociation(songFile: String): LyricsAssociation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyricsAssociation(association: LyricsAssociation)

    @Query("DELETE FROM lyrics_association WHERE songFile = :songFile")
    suspend fun deleteLyricsAssociation(songFile: String)

    @Query("SELECT * FROM lyrics_association")
    suspend fun getAllAssociations(): List<LyricsAssociation>

    @Query("DELETE FROM lyrics_association")
    suspend fun clearAllLyrics()

    // Favorites
    @Query("SELECT songId FROM favorites")
    fun getFavoriteSongIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSong)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    // History
    @Insert
    suspend fun addHistory(history: PlayHistory)

    @Query("SELECT songId FROM play_history ORDER BY playedAt DESC LIMIT 50")
    fun getRecentSongIds(): Flow<List<Long>>

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    // Playlists
    @Query("SELECT * FROM custom_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<CustomPlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: CustomPlaylist): Long

    @Query("DELETE FROM custom_playlists WHERE playlistId = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(ref: PlaylistSongCrossRef)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Long>>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
}
