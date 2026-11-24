package com.auramusic.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int
)

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: String): SongEntity?

    @Query("SELECT * FROM songs ORDER BY added_at DESC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM songs s INNER JOIN favorites f ON f.song_id = s.id ORDER BY f.added_at DESC")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM songs s INNER JOIN history h ON h.song_id = s.id " +
        "GROUP BY s.id ORDER BY MAX(h.played_at) DESC LIMIT 100")
    fun observeHistory(): Flow<List<SongEntity>>

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT p.id, p.name, p.created_at AS createdAt, " +
        "(SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlist_id = p.id) AS songCount " +
        "FROM playlists p ORDER BY p.created_at DESC")
    fun observeAllWithCount(): Flow<List<PlaylistCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON ps.song_id = s.id " +
        "WHERE ps.playlist_id = :playlistId ORDER BY ps.position")
    fun observeSongs(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(crossRef: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSong(playlistId: Long, songId: String)

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun clearSongs(playlistId: Long)
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun remove(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavorite(songId: String): Boolean
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}
