package com.auramusic.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.auramusic.presentation.components.AuroraBackground
import com.auramusic.presentation.components.AddToPlaylistButton
import com.auramusic.presentation.components.HomeHero
import com.auramusic.presentation.components.SectionHeader
import com.auramusic.presentation.components.SongRow

@Composable
fun HomeScreen(playerViewModel: PlayerViewModel) {
    val viewModel: HomeViewModel = hiltViewModel()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    AuroraBackground {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HomeHero(isPlaying = playerState.isPlaying) }
            if (recent.isNotEmpty()) {
                item { SectionHeader("Recently played") }
                itemsIndexed(recent.take(8), key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        title = song.title,
                        subtitle = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        durationMs = song.durationMs,
                        index = index,
                        trailing = { AddToPlaylistButton(song) }
                    ) {
                        playerViewModel.play(song, recent)
                    }
                }
            }
            if (favorites.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.favorites)) }
                itemsIndexed(favorites.take(6), key = { _, song -> "fav_${song.id}" }) { index, song ->
                    SongRow(
                        title = song.title,
                        subtitle = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        durationMs = song.durationMs,
                        index = index,
                        trailing = { AddToPlaylistButton(song) }
                    ) {
                        playerViewModel.play(song, favorites)
                    }
                }
            }
            item { SectionHeader("Trending now") }
            if (trending.isEmpty()) {
                item {
                    Text(
                        "Trending is quiet right now — search for something instead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                itemsIndexed(trending, key = { _, song -> "tr_${song.id}" }) { index, song ->
                    SongRow(
                        title = song.title,
                        subtitle = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        durationMs = song.durationMs,
                        index = index,
                        trailing = { AddToPlaylistButton(song) }
                    ) {
                        playerViewModel.play(song, trending)
                    }
                }
            }
        }
    }
}
