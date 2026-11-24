package com.auramusic.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.R
import com.auramusic.models.SearchFilter
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.components.SectionHeader
import com.auramusic.presentation.components.SongRow

@Composable
fun SearchScreen(playerViewModel: PlayerViewModel) {
    val viewModel: SearchViewModel = hiltViewModel()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
            SearchFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { viewModel.setFilter(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        if (query.isBlank()) {
            LazyColumn {
                if (recent.isNotEmpty()) {
                    item {
                        SectionHeader(
                            stringResource(R.string.recent_searches),
                            action = "Clear"
                        ) { viewModel.clearRecent() }
                    }
                    items(recent, key = { it }) { term ->
                        Text(
                            term,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onQueryChange(term); viewModel.searchNow() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                if (suggestions.isNotEmpty()) {
                    item { SectionHeader("Suggestions") }
                    items(suggestions.take(6)) { suggestion ->
                        Text(
                            suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onQueryChange(suggestion); viewModel.searchNow() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            LazyColumn {
                items(results, key = { it.id }) { song ->
                    SongRow(song.title, song.artist, song.thumbnailUrl, song.durationMs) {
                        playerViewModel.play(song, results)
                    }
                }
            }
        }
    }
}
