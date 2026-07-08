package com.cadence.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.components.SongRow
import com.cadence.player.ui.theme.SecondaryTxt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val album = remember(albumId) { repository.albumById(albumId) }
    val songs = remember(albumId) { repository.songsForAlbum(albumId) }
    val pState by player.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title            = {},
                navigationIcon   = {
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
            // ── Album header ────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArt(
                        albumId  = albumId,
                        cache    = artworkCache,
                        modifier = Modifier.size(220.dp),
                        cornerDp = 12,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text     = album?.title ?: "Unknown Album",
                        style    = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        text  = album?.artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if ((album?.year ?: 0) > 0) {
                        Text(
                            text  = "${album?.year} · ${songs.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryTxt,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // Play / Shuffle buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick  = { player.playSongs(songs, 0); onOpenNowPlaying() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick  = {
                                player.playSongs(songs.shuffled(), 0)
                                onOpenNowPlaying()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }

            // ── Track list ───────────────────────────────────────────────
            itemsIndexed(songs) { idx, song ->
                SongRow(
                    song             = song,
                    artworkCache     = artworkCache,
                    showArt          = false,
                    isPlaying        = pState.currentSong?.id == song.id,
                    onClick          = { player.playSongs(songs, idx); onOpenNowPlaying() },
                    onFavoriteToggle = { repository.toggleFavorite(song.id) },
                    onPlayNext       = { player.playNext(song) },
                    onAddToQueue     = { player.addToQueue(song) },
                )
                HorizontalDivider(
                    color    = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
