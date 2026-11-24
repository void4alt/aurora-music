package com.auramusic.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import com.auramusic.repository.MusicRepository
import com.auramusic.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _results = MutableStateFlow<List<Song>>(emptyList())
    val results: StateFlow<List<Song>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    val recent = settings.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _query
                .debounce(450L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collectLatest { searchNow() }
        }
        viewModelScope.launch {
            _query
                .debounce(250L)
                .distinctUntilChanged()
                .collectLatest { q ->
                    _suggestions.value = if (q.isBlank()) emptyList() else musicRepository.suggestions(q)
                }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        if (value.isBlank()) {
            _results.value = emptyList()
            _error.value = null
        }
    }

    fun setFilter(value: SearchFilter) {
        _filter.value = value
        if (_query.value.isNotBlank()) searchNow()
    }

    fun searchNow() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            musicRepository.search(q, _filter.value)
                .onSuccess { songs ->
                    _results.value = songs
                    settings.addRecentSearch(q)
                }
                .onFailure { throwable ->
                    _results.value = emptyList()
                    _error.value = throwable.message ?: "Search failed"
                }
            _loading.value = false
        }
    }

    fun clearRecent() {
        viewModelScope.launch { settings.clearRecentSearches() }
    }
}
