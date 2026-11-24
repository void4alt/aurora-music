package com.auramusic.presentation.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.auramusic.R
import com.auramusic.models.PlayerUiState
import com.auramusic.network.formatDuration
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.theme.AuroraPrimary
import com.auramusic.presentation.theme.AuroraSecondary
import com.auramusic.presentation.components.AuroraBackground
import com.auramusic.presentation.components.CatMascot
import com.auramusic.presentation.components.EqualizerBars
import com.auramusic.presentation.components.PulsingArtwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSpeed by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(state.positionMs, state.durationMs) {
        if (!dragging) sliderPosition = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat())
    }

    AuroraBackground {
        state.currentSong?.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(48.dp))
                CatMascot(
                    modifier = Modifier.size(52.dp),
                    isPlaying = state.isPlaying
                )
            }

            Spacer(Modifier.height(12.dp))
            val artwork = state.currentSong?.thumbnailUrl
            if (artwork.isNullOrBlank()) {
                Box(
                    Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AuroraPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    CatMascot(
                        modifier = Modifier.size(190.dp),
                        isPlaying = state.isPlaying
                    )
                }
            } else {
                PulsingArtwork(
                    url = artwork,
                    size = 260.dp,
                    radius = 28.dp,
                    isPlaying = state.isPlaying
                )
            }

            Spacer(Modifier.height(24.dp))
            val song = state.currentSong
            Text(
                song?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqualizerBars(
                    isPlaying = state.isPlaying,
                    modifier = Modifier.size(width = 20.dp, height = 14.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    song?.artist ?: "Pick something from your library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuroraSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))
            Slider(
                value = sliderPosition,
                onValueChange = { dragging = true; sliderPosition = it },
                onValueChangeFinished = {
                    dragging = false
                    viewModel.seekTo(sliderPosition.toLong())
                },
                valueRange = 0f..maxOf(1f, state.durationMs.toFloat()),
                enabled = state.durationMs > 0L,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))
            val playScale by animateFloatAsState(
                targetValue = if (state.isPlaying) 1.08f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "playScale"
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setShuffle(!state.shuffle) }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (state.shuffle) AuroraPrimary else Color.White
                    )
                }
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous), modifier = Modifier.size(36.dp))
                }
                IconButton(
                    onClick = { viewModel.togglePlay() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(56.dp),
                        tint = AuroraPrimary
                    )
                }
                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next), modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.cycleRepeat() }) {
                    Icon(
                        imageVector = if (state.repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.repeat),
                        tint = if (state.repeatMode != 0) AuroraPrimary else Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { song?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.favorite))
                }
                IconButton(onClick = { showSpeed = true }) {
                    Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.playback_speed))
                }
                IconButton(onClick = { showSleep = true }) {
                    Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.sleep_timer))
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = stringResource(R.string.queue))
                }
            }

            if (song != null && !song.isPlayableInApp) {
                Text(
                    stringResource(R.string.direct_playback_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showSpeed) {
        AlertDialog(
            onDismissRequest = { showSpeed = false },
            title = { Text(stringResource(R.string.playback_speed)) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        TextButton(onClick = {
                            viewModel.setSpeed(speed)
                            showSpeed = false
                        }) {
                            Text("${speed}x")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeed = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 60).forEach { minutes ->
                        TextButton(onClick = {
                            viewModel.setSleepTimer(minutes)
                            showSleep = false
                        }) { Text("$minutes min") }
                    }
                    TextButton(onClick = {
                        viewModel.clearSleepTimer()
                        showSleep = false
                    }) { Text("Off") }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text(stringResource(R.string.queue)) },
            text = {
                Column {
                    state.queue.forEach { queued ->
                        Text(
                            "${queued.title} — ${queued.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.play(queued, state.queue)
                                    showQueue = false
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                    if (state.queue.isEmpty()) {
                        Text("Queue is empty", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
