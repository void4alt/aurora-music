package com.auramusic.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.models.Song
import com.auramusic.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistPickerViewModel @Inject constructor(
    private val library: LibraryRepository
) : ViewModel() {

    val playlists = library.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { library.addSongToPlaylist(playlistId, song) }
    }

    fun createAndAdd(name: String, song: Song) {
        viewModelScope.launch {
            val id = library.createPlaylist(name)
            library.addSongToPlaylist(id, song)
        }
    }
}
