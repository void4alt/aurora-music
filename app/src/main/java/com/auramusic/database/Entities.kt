package com.auramusic.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    @ColumnInfo(name = "video_id") val videoId: String? = null,
    @ColumnInfo(name = "media_url") val mediaUrl: String? = null,
    val source: String = "YOUTUBE",
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "song_id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("song_id"), Index("playlist_id")]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "song_id") val songId: String,
    val position: Int = 0
)

@Entity(tableName = "favorites", primaryKeys = ["song_id"])
data class FavoriteEntity(
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "played_at") val playedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithSongsRelation(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<SongEntity>
)
