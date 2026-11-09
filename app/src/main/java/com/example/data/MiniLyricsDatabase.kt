package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LyricsAssociation::class,
        FavoriteSong::class,
        PlayHistory::class,
        CustomPlaylist::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MiniLyricsDatabase : RoomDatabase() {

    abstract fun dao(): MiniLyricsDao

    companion object {
        @Volatile
        private var INSTANCE: MiniLyricsDatabase? = null

        fun getDatabase(context: Context): MiniLyricsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MiniLyricsDatabase::class.java,
                    "minilyrics.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
