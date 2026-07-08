package com.cadence.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.model.Playlist
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.components.SongRow
import com.cadence.player.ui.theme.SecondaryTxt

private val tabs = listOf("Songs", "Albums", "Artists", "Playlists")

@Composable
fun LibraryScreen(
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val songs     by repository.songs.collectAsState()
    val albums    by repository.albums.collectAsState()
    val artists   by repository.artists.collectAsState()
    val playlists by repository.playlists.collectAsState()
    val pState    by player.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text     = "Library",
            style    = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding      = 16.dp,
            containerColor   = MaterialTheme.colorScheme.background,
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = idx == selectedTab,
                    onClick  = { selectedTab = idx },
                    text     = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> SongsTab(
                songs, artworkCache,
                currentSongId = pState.currentSong?.id,
                onSongClick   = { idx ->
                    player.playSongs(songs, idx); onOpenNowPlaying()
                },
                onFavorite    = { repository.toggleFavorite(it) },
                onPlayNext    = { player.playNext(it) },
                onAddToQueue  = { player.addToQueue(it) },
            )
            1 -> AlbumsTab(albums, artworkCache, onAlbumClick)
            2 -> ArtistsTab(artists, onArtistClick)
            3 -> PlaylistsTab(playlists, onPlaylistClick, onCreate = { name ->
                repository.createPlaylist(name)
            })
        }
    }
}

// ── Songs ─────────────────────────────────────────────────────────────────

@Composable
private fun SongsTab(
    songs: List<com.cadence.player.model.Song>,
    artworkCache: ArtworkCache,
    currentSongId: Long?,
    onSongClick: (Int) -> Unit,
    onFavorite: (Long) -> Unit,
    onPlayNext: (com.cadence.player.model.Song) -> Unit,
    onAddToQueue: (com.cadence.player.model.Song) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs) { idx, song ->
            SongRow(
                song             = song,
                artworkCache     = artworkCache,
                isPlaying        = song.id == currentSongId,
                onClick          = { onSongClick(idx) },
                onFavoriteToggle = { onFavorite(song.id) },
                onPlayNext       = { onPlayNext(song) },
                onAddToQueue     = { onAddToQueue(song) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                modifier = Modifier.padding(start = 76.dp))
        }
    }
}

// ── Albums ────────────────────────────────────────────────────────────────

@Composable
private fun AlbumsTab(
    albums: List<com.cadence.player.model.Album>,
    artworkCache: ArtworkCache,
    onAlbumClick: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns        = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums) { album ->
            Column(modifier = Modifier.clickable { onAlbumClick(album.id) }) {
                AlbumArt(
                    albumId  = album.id,
                    cache    = artworkCache,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Spacer(Modifier.height(6.dp))
                Text(album.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, style = MaterialTheme.typography.bodySmall,
                    color = SecondaryTxt, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Artists ───────────────────────────────────────────────────────────────

@Composable
private fun ArtistsTab(
    artists: List<com.cadence.player.model.Artist>,
    onArtistClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists) { artist ->
            ListItem(
                headlineContent   = { Text(artist.name) },
                supportingContent = { Text("${artist.albumCount} albums · ${artist.songCount} songs", color = SecondaryTxt) },
                modifier          = Modifier.clickable { onArtistClick(artist.name) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp))
        }
    }
}

// ── Playlists ─────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName    by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title   = { Text("New Playlist") },
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
                    if (newName.isNotBlank()) { onCreate(newName.trim()); newName = "" }
                    showDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newName = "" }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent   = { Text("New Playlist", color = MaterialTheme.colorScheme.primary) },
                leadingContent    = {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable { showDialog = true },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }

        items(playlists) { playlist ->
            ListItem(
                headlineContent   = { Text(playlist.name) },
                supportingContent = { Text("${playlist.songIds.size} songs", color = SecondaryTxt) },
                modifier          = Modifier.clickable { onPlaylistClick(playlist.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp))
        }
    }
}
