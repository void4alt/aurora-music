package com.auramusic.repository

import com.auramusic.models.ImportedPlaylist
import com.auramusic.models.Playlist
import com.auramusic.models.PlaylistWithSongs
import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun search(query: String, filter: SearchFilter): Result<List<Song>>
    suspend fun trending(): Result<List<Song>>
    suspend fun suggestions(query: String): List<String>
    suspend fun playlist(link: String): Result<ImportedPlaylist>
}

interface LibraryRepository {
    fun observeSongs(): Flow<List<Song>>
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylist(id: Long): Flow<PlaylistWithSongs?>
    fun observeFavorites(): Flow<List<Song>>
    fun observeHistory(): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun addSongToPlaylist(playlistId: Long, song: Song)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    suspend fun saveSong(song: Song)
    suspend fun toggleFavorite(song: Song): Boolean
    suspend fun isFavorite(songId: String): Boolean
    suspend fun recordPlay(song: Song)
    suspend fun clearHistory()

    suspend fun addDirectUrl(title: String, url: String): Song
    suspend fun addLocalFile(title: String, uri: String): Song
}
