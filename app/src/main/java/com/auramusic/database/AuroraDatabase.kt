package com.auramusic.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun build(context: Context): AuroraDatabase = Room
            .databaseBuilder(context.applicationContext, AuroraDatabase::class.java, "aurora.db")
            .build()
    }
}
