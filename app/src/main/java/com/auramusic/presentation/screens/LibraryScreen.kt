package com.auramusic.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.R
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.components.AddToPlaylistButton
import com.auramusic.presentation.components.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(playerViewModel: PlayerViewModel) {
    val viewModel: LibraryViewModel = hiltViewModel()
    val context = LocalContext.current

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlTitle by remember { mutableStateOf("") }
    var urlValue by remember { mutableStateOf("") }
    var linkInput by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "Local audio"
            viewModel.addLocalFile(name, uri.toString())
        }
    }

    if (selectedPlaylist != null) {
        val detail = selectedPlaylist!!
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(detail.playlist.name, style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn {
                items(detail.songs, key = { it.id }) { song ->
                    SongRow(
                        title = song.title,
                        subtitle = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        durationMs = song.durationMs,
                        trailing = {
                            Row {
                                AddToPlaylistButton(song)
                                IconButton(onClick = { viewModel.removeFromPlaylist(detail.playlist.id, song.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    ) { playerViewModel.play(song, detail.songs) }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.library)) },
            actions = {
                IconButton(onClick = {
                    editingId = null
                    nameInput = ""
                    showPlaylistDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_playlist))
                }
                IconButton(onClick = { filePicker.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Filled.Audiotrack, contentDescription = "Add local audio")
                }
                IconButton(onClick = { showUrlDialog = true }) {
                    Icon(Icons.Filled.Link, contentDescription = "Add audio URL")
                }
                IconButton(onClick = {
                    linkInput = ""
                    showLinkDialog = true
                }) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Import playlist link")
                }
            }
        )

        TabRow(selectedTabIndex = tab) {
            listOf(stringResource(R.string.playlists), stringResource(R.string.favorites), stringResource(R.string.history))
                .forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
        }

        when (tab) {
            0 -> LazyColumn(Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { viewModel.selectPlaylist(playlist.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${playlist.songCount} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editingId = playlist.id
                                nameInput = playlist.name
                                showPlaylistDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                            }
                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
            1 -> LazyColumn(Modifier.fillMaxSize()) {
                if (favorites.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_library))
                    } }
                }
                items(favorites, key = { "fav_${it.id}" }) { song ->
                    SongRow(
                        song.title,
                        song.artist,
                        song.thumbnailUrl,
                        song.durationMs,
                        trailing = { AddToPlaylistButton(song) }
                    ) {
                        playerViewModel.play(song, favorites)
                    }
                }
            }
            2 -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear listening history")
                        }
                    }
                }
                items(history, key = { "hist_${it.id}" }) { song ->
                    SongRow(
                        song.title,
                        song.artist,
                        song.thumbnailUrl,
                        song.durationMs,
                        trailing = { AddToPlaylistButton(song) }
                    ) {
                        playerViewModel.play(song, history)
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text(if (editingId == null) stringResource(R.string.new_playlist) else stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingId
                    if (id == null) viewModel.createPlaylist(nameInput)
                    else viewModel.renamePlaylist(id, nameInput)
                    showPlaylistDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Add audio stream") },
            text = {
                Column {
                    OutlinedTextField(
                        value = urlTitle,
                        onValueChange = { urlTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = urlValue,
                        onValueChange = { urlValue = it },
                        label = { Text("Direct audio URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addUrl(urlTitle, urlValue)
                    urlTitle = ""
                    urlValue = ""
                    showUrlDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Import playlist link") },
            text = {
                Column {
                    Text(
                        "Paste a YouTube playlist link, YouTube Music link, or a single video link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        label = { Text("Playlist link") },
                        placeholder = { Text("https://youtube.com/playlist?list=...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importPlaylist(linkInput)
                    showLinkDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
