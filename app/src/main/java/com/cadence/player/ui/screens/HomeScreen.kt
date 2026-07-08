package com.cadence.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.model.Song
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.components.SectionHeader
import com.cadence.player.ui.theme.SecondaryTxt

@Composable
fun HomeScreen(
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onOpenNowPlaying: () -> Unit,
) {
    // Bug 3 fix: collect BOTH songs and recent so carousels update reactively
    val songs  by repository.songs.collectAsState()
    val recent by repository.recent.collectAsState()

    // Derive display lists; recompute whenever songs or recent changes
    val recentlyPlayed = remember(songs, recent) { repository.recentlyPlayed() }
    val recentlyAdded  = remember(songs)         { repository.recentlyAdded() }
    val favorites      = remember(songs)         { repository.favoriteSongs() }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier       = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text     = "Listen Now",
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
            )
        }

        // Recently Played
        if (recentlyPlayed.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                SongCarousel(
                    songs        = recentlyPlayed,
                    artworkCache = artworkCache,
                    onSongClick  = { idx ->
                        player.playSongs(recentlyPlayed, idx)
                        onOpenNowPlaying()
                    },
                )
            }
        }

        // Recently Added
        if (recentlyAdded.isNotEmpty()) {
            item { SectionHeader("Recently Added") }
            item {
                SongCarousel(
                    songs        = recentlyAdded,
                    artworkCache = artworkCache,
                    onSongClick  = { idx ->
                        player.playSongs(recentlyAdded, idx)
                        onOpenNowPlaying()
                    },
                )
            }
        }

        // Favorites
        if (favorites.isNotEmpty()) {
            item { SectionHeader("Favorites") }
            item {
                SongCarousel(
                    songs        = favorites,
                    artworkCache = artworkCache,
                    onSongClick  = { idx ->
                        player.playSongs(favorites, idx)
                        onOpenNowPlaying()
                    },
                )
            }
        }

        // Empty state
        if (recentlyPlayed.isEmpty() && recentlyAdded.isEmpty() && favorites.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text      = "No music found.\nGrant storage permission to get started.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = SecondaryTxt,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SongCarousel(
    songs: List<Song>,
    artworkCache: ArtworkCache,
    onSongClick: (Int) -> Unit,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(songs) { idx, song ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onSongClick(idx) },
            ) {
                AlbumArt(
                    albumId  = song.albumId,
                    cache    = artworkCache,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    cornerDp = 8,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = SecondaryTxt,
                )
            }
        }
    }
}
