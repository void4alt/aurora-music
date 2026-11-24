package com.auramusic.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.models.PlaylistWithSongs
import com.auramusic.models.Song
import com.auramusic.repository.LibraryRepository
import com.auramusic.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playlists = libraryRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites = libraryRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history = libraryRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId

    val selectedPlaylist: StateFlow<PlaylistWithSongs?> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else libraryRepository.observePlaylist(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun selectPlaylist(id: Long?) { _selectedPlaylistId.value = id }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) libraryRepository.createPlaylist(name)
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) libraryRepository.renamePlaylist(id, name)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            libraryRepository.deletePlaylist(id)
            if (_selectedPlaylistId.value == id) _selectedPlaylistId.value = null
        }
    }

    fun addUrl(title: String, url: String) {
        viewModelScope.launch {
            if (url.isBlank()) {
                _message.tryEmit("Enter an audio URL")
                return@launch
            }
            runCatching { libraryRepository.addDirectUrl(title, url) }
                .onSuccess { _message.tryEmit("Stream saved to your library") }
                .onFailure { _message.tryEmit(it.message ?: "Could not save stream") }
        }
    }

    fun addLocalFile(title: String, uri: String) {
        viewModelScope.launch {
            runCatching { libraryRepository.addLocalFile(title, uri) }
                .onSuccess { _message.tryEmit("Local file saved to your library") }
                .onFailure { _message.tryEmit(it.message ?: "Could not save file") }
        }
    }

    fun removeFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch { libraryRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { libraryRepository.addSongToPlaylist(playlistId, song) }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { libraryRepository.toggleFavorite(song) }
    }

    fun clearHistory() {
        viewModelScope.launch { libraryRepository.clearHistory() }
    }

    fun importPlaylist(link: String) {
        viewModelScope.launch {
            if (link.isBlank()) {
                _message.tryEmit("Paste a YouTube playlist link")
                return@launch
            }
            runCatching { musicRepository.playlist(link).getOrThrow() }
                .onSuccess { imported ->
                    if (imported.songs.isEmpty()) {
                        _message.tryEmit("No tracks found in that playlist")
                    } else {
                        val id = libraryRepository.createPlaylist(imported.name)
                        imported.songs.forEach { libraryRepository.addSongToPlaylist(id, it) }
                        _message.tryEmit("Imported ${imported.songs.size} songs into ${imported.name}")
                        selectPlaylist(id)
                    }
                }
                .onFailure { _message.tryEmit(it.message ?: "Could not import that playlist") }
        }
    }
}
