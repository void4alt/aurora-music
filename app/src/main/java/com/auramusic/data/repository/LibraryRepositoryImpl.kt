package com.auramusic.data.repository

import com.auramusic.data.mapper.toEntity
import com.auramusic.data.mapper.toSong
import com.auramusic.database.AuroraDatabase
import com.auramusic.database.FavoriteEntity
import com.auramusic.database.HistoryEntity
import com.auramusic.database.PlaylistEntity
import com.auramusic.database.PlaylistSongEntity
import com.auramusic.models.Playlist
import com.auramusic.models.PlaylistWithSongs
import com.auramusic.models.Song
import com.auramusic.models.SongSource
import com.auramusic.repository.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibraryRepositoryImpl @Inject constructor(
    database: AuroraDatabase
) : LibraryRepository {

    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val historyDao = database.historyDao()

    override fun observeSongs(): Flow<List<Song>> = songDao.observeAll().map { rows -> rows.map { it.toSong() } }

    override fun observePlaylists(): Flow<List<Playlist>> = playlistDao.observeAllWithCount().map { rows ->
        rows.map { Playlist(it.id, it.name, it.createdAt, it.songCount) }
    }

    override fun observePlaylist(id: Long): Flow<PlaylistWithSongs?> =
        playlistDao.observeById(id).flatMapLatest { playlist ->
            if (playlist == null) {
                flowOf(null)
            } else {
                playlistDao.observeSongs(id).map { rows ->
                    PlaylistWithSongs(
                        playlist = Playlist(playlist.id, playlist.name, playlist.createdAt, rows.size),
                        songs = rows.map { it.toSong() }
                    )
                }
            }
        }

    override fun observeFavorites(): Flow<List<Song>> =
        songDao.observeFavorites().map { rows -> rows.map { it.toSong() } }

    override fun observeHistory(): Flow<List<Song>> =
        songDao.observeHistory().map { rows -> rows.map { it.toSong() } }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insert(PlaylistEntity(name = name.trim().ifBlank { "New playlist" }))
    }

    override suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        playlistDao.get(id)?.let { playlistDao.update(it.copy(name = name.trim())) }
        Unit
    }

    override suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.get(id)?.let { playlistDao.delete(it) }
        Unit
    }

    override suspend fun addSongToPlaylist(playlistId: Long, song: Song) = withContext(Dispatchers.IO) {
        songDao.upsert(song.toEntity())
        val position = playlistDao.nextPosition(playlistId)
        playlistDao.addSong(PlaylistSongEntity(playlistId, song.id, position))
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSong(playlistId, songId)
    }

    override suspend fun saveSong(song: Song) = withContext(Dispatchers.IO) { songDao.upsert(song.toEntity()) }

    override suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        songDao.upsert(song.toEntity())
        if (favoriteDao.isFavorite(song.id)) {
            favoriteDao.remove(song.id)
            false
        } else {
            favoriteDao.add(FavoriteEntity(song.id))
            true
        }
    }

    override suspend fun isFavorite(songId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(songId)
    }

    override suspend fun recordPlay(song: Song) = withContext(Dispatchers.IO) {
        songDao.upsert(song.toEntity())
        historyDao.add(HistoryEntity(songId = song.id))
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) { historyDao.clear() }

    override suspend fun addDirectUrl(title: String, url: String): Song = withContext(Dispatchers.IO) {
        val song = Song(
            id = "url_${url.hashCode()}",
            title = title.trim().ifBlank { "Audio stream" },
            artist = "Direct stream",
            mediaUrl = url.trim(),
            source = SongSource.DIRECT_URL
        )
        songDao.upsert(song.toEntity())
        song
    }

    override suspend fun addLocalFile(title: String, uri: String): Song = withContext(Dispatchers.IO) {
        val song = Song(
            id = "local_${uri.hashCode()}",
            title = title.trim().ifBlank { "Local audio" },
            artist = "Local file",
            mediaUrl = uri,
            source = SongSource.LOCAL_FILE
        )
        songDao.upsert(song.toEntity())
        song
    }
}
