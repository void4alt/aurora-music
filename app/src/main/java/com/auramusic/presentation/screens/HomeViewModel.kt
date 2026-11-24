package com.auramusic.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.models.Song
import com.auramusic.repository.LibraryRepository
import com.auramusic.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    musicRepository: MusicRepository,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _trending = MutableStateFlow<List<Song>>(emptyList())
    val trending: StateFlow<List<Song>> = _trending.asStateFlow()

    val recent = libraryRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Song>())

    val favorites = libraryRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Song>())

    init {
        viewModelScope.launch {
            musicRepository.trending()
                .onSuccess { _trending.value = it }
                .onFailure { _trending.value = emptyList() }
        }
    }
}
