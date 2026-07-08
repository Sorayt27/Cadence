package com.cadence.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.components.SectionHeader
import com.cadence.player.ui.components.SongRow
import com.cadence.player.ui.theme.SecondaryTxt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onAlbumClick: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    // Bug 2 fix: collect songs and albums as reactive state, not one-shot snapshots
    val allSongs  by repository.songs.collectAsState()
    val allAlbums by repository.albums.collectAsState()
    val pState    by player.state.collectAsState()

    val songs  = remember(allSongs, artistName)  { allSongs.filter  { it.artist == artistName }.sortedBy { it.title } }
    val albums = remember(allAlbums, artistName) { allAlbums.filter { it.artist == artistName } }
    val artist = remember(allSongs, artistName)  { repository.artistByName(artistName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text(artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text      = artistName,
                        style     = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text  = "${artist?.albumCount ?: albums.size} albums · ${songs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryTxt,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick  = { if (songs.isNotEmpty()) { player.playSongs(songs, 0); onOpenNowPlaying() } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick  = { if (songs.isNotEmpty()) { player.playSongs(songs.shuffled(), 0); onOpenNowPlaying() } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }

            // ── Albums ──────────────────────────────────────────────────────
            if (albums.isNotEmpty()) {
                item { SectionHeader("Albums") }
                item {
                    val gridHeight = if (albums.size > 2) 380.dp else 200.dp
                    LazyHorizontalGrid(
                        rows                  = GridCells.Fixed(if (albums.size > 2) 2 else 1),
                        modifier              = Modifier.fillMaxWidth().height(gridHeight),
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                    ) {
                        items(albums) { album ->
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onAlbumClick(album.id) },
                            ) {
                                AlbumArt(
                                    albumId  = album.id,
                                    cache    = artworkCache,
                                    modifier = Modifier.size(140.dp),
                                    cornerDp = 8,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    album.title,
                                    style    = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (album.year > 0) {
                                    Text(
                                        "${album.year}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryTxt,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Songs ───────────────────────────────────────────────────────
            item {
                SectionHeader("Songs")
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
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp,
                    modifier  = Modifier.padding(start = 76.dp),
                )
            }
        }
    }
}
