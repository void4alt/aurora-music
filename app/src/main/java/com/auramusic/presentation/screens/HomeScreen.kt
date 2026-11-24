package com.auramusic.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.R
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.components.SectionHeader
import com.auramusic.presentation.components.SongRow

@Composable
fun HomeScreen(playerViewModel: PlayerViewModel) {
    val viewModel: HomeViewModel = hiltViewModel()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }
        if (recent.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            items(recent.take(8), key = { it.id }) { song ->
                SongRow(song.title, song.artist, song.thumbnailUrl, song.durationMs) {
                    playerViewModel.play(song, recent)
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.favorites)) }
            items(favorites.take(6), key = { "fav_${it.id}" }) { song ->
                SongRow(song.title, song.artist, song.thumbnailUrl, song.durationMs) {
                    playerViewModel.play(song, favorites)
                }
            }
        }
        item { SectionHeader("Trending now") }
        if (trending.isEmpty()) {
            item {
                Text(
                    "Trending is unavailable. Add a YouTube API key to load recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(trending, key = { "tr_${it.id}" }) { song ->
                SongRow(song.title, song.artist, song.thumbnailUrl, song.durationMs) {
                    playerViewModel.play(song, trending)
                }
            }
        }
    }
}
