package com.auramusic.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.models.Song
import com.auramusic.presentation.screens.PlaylistPickerViewModel
import com.auramusic.presentation.theme.AuroraPrimary

/** One-tap "add to playlist" control usable from every song list. */
@Composable
fun AddToPlaylistButton(song: Song, modifier: Modifier = Modifier) {
    val viewModel: PlaylistPickerViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var show by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    IconButton(onClick = { show = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.PlaylistAdd,
            contentDescription = "Add to playlist",
            tint = AuroraPrimary
        )
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Add to playlist") },
            text = {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp)
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    if (playlists.isEmpty()) {
                        Text(
                            "No playlists yet — make one below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    playlists.forEach { playlist ->
                        TextButton(onClick = {
                            viewModel.addToPlaylist(playlist.id, song)
                            show = false
                        }) {
                            Text("+  ${playlist.name}")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("New playlist") },
                        singleLine = true
                    )
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createAndAdd(name, song)
                            name = ""
                            show = false
                        }
                    }) {
                        Text("Create and add")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show = false }) { Text("Done") }
            }
        )
    }
}
