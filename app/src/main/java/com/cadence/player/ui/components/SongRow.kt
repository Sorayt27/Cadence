package com.cadence.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.model.Song
import com.cadence.player.model.formattedDuration
import com.cadence.player.ui.theme.Red
import com.cadence.player.ui.theme.SecondaryTxt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    artworkCache: ArtworkCache,
    isPlaying: Boolean = false,
    showArt: Boolean = true,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = { menuExpanded = true },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArt) {
            AlbumArt(
                albumId  = song.albumId,
                cache    = artworkCache,
                modifier = Modifier.size(48.dp),
                cornerDp = 6,
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = song.title,
                style    = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = if (isPlaying) Red else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text     = "${song.artist} · ${song.formattedDuration()}",
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = SecondaryTxt,
            )
        }

        // Favorite icon
        if (onFavoriteToggle != null) {
            IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Red else SecondaryTxt,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Overflow menu
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SecondaryTxt,
                    modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                onPlayNext?.let {
                    DropdownMenuItem(text = { Text("Play Next") }, onClick = { it(); menuExpanded = false })
                }
                onAddToQueue?.let {
                    DropdownMenuItem(text = { Text("Add to Queue") }, onClick = { it(); menuExpanded = false })
                }
                onFavoriteToggle?.let {
                    DropdownMenuItem(
                        text    = { Text(if (song.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                        onClick = { it(); menuExpanded = false },
                    )
                }
                onAddToPlaylist?.let {
                    DropdownMenuItem(text = { Text("Add to Playlist…") }, onClick = { it(); menuExpanded = false })
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
