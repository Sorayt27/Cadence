package com.cadence.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.player.data.ArtworkCache
import com.cadence.player.playback.PlaybackState
import com.cadence.player.ui.theme.Card
import com.cadence.player.ui.theme.Red

@Composable
fun MiniPlayer(
    state: PlaybackState,
    artworkCache: ArtworkCache,
    onTap: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.currentSong != null,
        enter   = slideInVertically { it },
        exit    = slideOutVertically { it },
        modifier = modifier,
    ) {
        val song = state.currentSong ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Card)
                .clickable(onClick = onTap),
        ) {
            // progress bar at bottom of the card
            if (state.durationMs > 0) {
                LinearProgressIndicator(
                    progress    = { (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) },
                    modifier    = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                    color       = Red,
                    trackColor  = MaterialTheme.colorScheme.outline,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(
                    albumId  = song.albumId,
                    cache    = artworkCache,
                    modifier = Modifier.size(44.dp),
                    cornerDp = 6,
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = song.title,
                        style    = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color    = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = song.artist,
                        style    = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector        = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
