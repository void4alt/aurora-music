package com.auramusic.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import com.auramusic.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    val categories = listOf(
        "Lo-fi beats", "Jazz", "Rock", "Hip hop",
        "Live sessions", "Podcasts", "Remixes", "Ambient"
    )

    private val _results = MutableStateFlow<List<Song>>(emptyList())
    val results: StateFlow<List<Song>> = _results.asStateFlow()

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(category: String) {
        _selected.value = category
        _loading.value = true
        viewModelScope.launch {
            musicRepository.search(category, SearchFilter.MUSIC)
                .onSuccess { _results.value = it }
                .onFailure { _results.value = emptyList() }
            _loading.value = false
        }
    }

    fun clear() {
        _selected.value = null
        _results.value = emptyList()
    }
}
