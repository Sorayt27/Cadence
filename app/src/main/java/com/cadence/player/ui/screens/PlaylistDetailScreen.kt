package com.cadence.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.SongRow
import com.cadence.player.ui.theme.SecondaryTxt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val playlists by repository.playlists.collectAsState()
    val playlist  = playlists.find { it.id == playlistId }
    val songs     = remember(playlist) { playlist?.let { repository.songsForPlaylist(it) } ?: emptyList() }
    val pState    by player.state.collectAsState()

    var showRename  by remember { mutableStateOf(false) }
    var showDelete  by remember { mutableStateOf(false) }
    var newName     by remember { mutableStateOf(playlist?.name ?: "") }
    var menuExpanded by remember { mutableStateOf(false) }

    // Rename dialog
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title   = { Text("Rename Playlist") },
            text    = {
                OutlinedTextField(
                    value         = newName,
                    onValueChange = { newName = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) repository.renamePlaylist(playlistId, newName.trim())
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    // Delete confirmation
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title   = { Text("Delete Playlist") },
            text    = { Text("Delete \"${playlist?.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { repository.deletePlaylist(playlistId); onBack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text    = { Text("Rename") },
                                onClick = { newName = playlist?.name ?: ""; showRename = true; menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text    = { Text("Delete") },
                                onClick = { showDelete = true; menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error) },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = SecondaryTxt)
                    Spacer(Modifier.height(12.dp))
                    Text("This playlist is empty", color = SecondaryTxt)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Header actions
            item {
                Row(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick  = { player.playSongs(songs, 0); onOpenNowPlaying() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick  = { player.playSongs(songs.shuffled(), 0); onOpenNowPlaying() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
                Text(
                    "${songs.size} songs",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = SecondaryTxt,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }

            itemsIndexed(songs) { idx, song ->
                SongRow(
                    song             = song,
                    artworkCache     = artworkCache,
                    isPlaying        = pState.currentSong?.id == song.id,
                    onClick          = { player.playSongs(songs, idx); onOpenNowPlaying() },
                    onFavoriteToggle = { repository.toggleFavorite(song.id) },
                    onPlayNext       = { player.playNext(song) },
                    onAddToQueue     = { player.addToQueue(song) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 76.dp))
            }
        }
    }
}
