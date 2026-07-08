package com.cadence.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.model.Album
import com.cadence.player.model.Artist
import com.cadence.player.model.Song
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.components.SectionHeader
import com.cadence.player.ui.components.SongRow
import com.cadence.player.ui.theme.SecondaryTxt

@Composable
fun SearchScreen(
    repository: MusicRepository,
    player: PlayerController,
    artworkCache: ArtworkCache,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val (songResults, albumResults, artistResults) = remember(query) {
        repository.search(query)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text     = "Search",
            style    = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Songs, artists, albums…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = if (query.isNotEmpty()) {{
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }} else null,
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .focusRequester(focusRequester),
        )

        if (query.isBlank()) {
            // Show genre/mood chips or just a prompt
            Box(
                modifier         = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter,
            ) {
                Text("Start typing to search your library", color = SecondaryTxt,
                    style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }

        val hasSongs   = songResults.isNotEmpty()
        val hasAlbums  = albumResults.isNotEmpty()
        val hasArtists = artistResults.isNotEmpty()

        if (!hasSongs && !hasAlbums && !hasArtists) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter,
            ) {
                Text("No results for \"$query\"", color = SecondaryTxt,
                    style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Songs ────────────────────────────────────────────────────
            if (hasSongs) {
                item { SectionHeader("Songs") }
                itemsIndexed(songResults.take(5)) { idx, song ->
                    SongRow(
                        song             = song,
                        artworkCache     = artworkCache,
                        onClick          = { player.playSongs(songResults, idx); onOpenNowPlaying() },
                        onFavoriteToggle = { repository.toggleFavorite(song.id) },
                        onPlayNext       = { player.playNext(song) },
                        onAddToQueue     = { player.addToQueue(song) },
                    )
                }
                if (songResults.size > 5) {
                    item {
                        TextButton(
                            onClick  = { /* could expand or show all */ },
                            modifier = Modifier.padding(start = 12.dp),
                        ) { Text("See all ${songResults.size} songs") }
                    }
                }
            }

            // ── Albums ───────────────────────────────────────────────────
            if (hasAlbums) {
                item { SectionHeader("Albums") }
                items(albumResults.take(4)) { album ->
                    AlbumSearchRow(album, artworkCache) { onAlbumClick(album.id) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp))
                }
            }

            // ── Artists ──────────────────────────────────────────────────
            if (hasArtists) {
                item { SectionHeader("Artists") }
                items(artistResults.take(4)) { artist ->
                    ListItem(
                        headlineContent   = { Text(artist.name) },
                        supportingContent = { Text("${artist.songCount} songs", color = SecondaryTxt) },
                        modifier          = Modifier.clickable { onArtistClick(artist.name) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 16.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AlbumSearchRow(
    album: Album,
    artworkCache: ArtworkCache,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId  = album.id,
            cache    = artworkCache,
            modifier = Modifier.size(52.dp),
            cornerDp = 6,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(album.title, style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("${album.artist} · ${album.songCount} songs",
                style = MaterialTheme.typography.bodySmall, color = SecondaryTxt,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
