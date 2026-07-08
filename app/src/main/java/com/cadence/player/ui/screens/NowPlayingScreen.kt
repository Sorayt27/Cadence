package com.cadence.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.MusicRepository
import com.cadence.player.playback.PlayerController
import com.cadence.player.ui.components.AlbumArt
import com.cadence.player.ui.theme.Red
import com.cadence.player.ui.theme.SecondaryTxt
import kotlinx.coroutines.delay

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun NowPlayingScreen(
    player: PlayerController,
    repository: MusicRepository,
    artworkCache: ArtworkCache,
    onDismiss: () -> Unit,
) {
    val state by player.state.collectAsState()

    // Bug 5 fix: derive isFavorite from repository.songs (reactive) not player.state (stale)
    val songs by repository.songs.collectAsState()
    val song   = state.currentSong
    val isFavorite = remember(songs, song?.id) {
        songs.find { it.id == song?.id }?.isFavorite ?: false
    }

    // ── Live position ticker ──────────────────────────────────────────────
    var position by remember { mutableLongStateOf(state.positionMs) }
    LaunchedEffect(state.isPlaying, state.positionMs) {
        position = state.positionMs
        if (state.isPlaying) {
            while (true) {
                delay(500)
                position = player.getPosition()
            }
        }
    }

    var showQueue       by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    // Bug 4 fix: find correct index, default to 1x (index 2) if not found
    var speedIndex by remember {
        mutableIntStateOf(
            speeds.indexOf(state.playbackSpeed).let { if (it < 0) 2 else it }
        )
    }

    // ── Sleep timer ───────────────────────────────────────────────────────
    var sleepMinutes by remember { mutableIntStateOf(0) }
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            player.playPause()
            sleepMinutes = 0
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onSet     = { mins -> sleepMinutes = mins; showSleepDialog = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        modifier           = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.titleSmall, color = SecondaryTxt)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showQueue = !showQueue }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                }
            }

            if (!showQueue) {
                // ── Artwork ───────────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                AlbumArt(
                    albumId  = song?.albumId ?: -1L,
                    cache    = artworkCache,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    cornerDp = 16,
                )
                Spacer(Modifier.height(28.dp))

                // ── Title / artist / favorite ─────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = song?.title ?: "—",
                            style    = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text     = song?.artist ?: "—",
                            style    = MaterialTheme.typography.bodyLarge,
                            color    = SecondaryTxt,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Bug 5 fix: use isFavorite from repository.songs, not player state
                    IconButton(onClick = { song?.let { repository.toggleFavorite(it.id) } }) {
                        Icon(
                            imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint               = if (isFavorite) Red else SecondaryTxt,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Scrubber ──────────────────────────────────────────────
                val duration = state.durationMs.coerceAtLeast(1L)
                var dragging  by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }

                Slider(
                    value                 = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange         = { dragValue = it; dragging = true },
                    onValueChangeFinished = {
                        player.seekTo((dragValue * duration).toLong())
                        dragging = false
                    },
                    colors   = SliderDefaults.colors(
                        thumbColor         = Color.White,
                        activeTrackColor   = Color.White,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    val displayPos = if (dragging) (dragValue * duration).toLong() else position
                    Text(formatMs(displayPos), style = MaterialTheme.typography.labelSmall, color = SecondaryTxt)
                    Spacer(Modifier.weight(1f))
                    Text(formatMs(state.durationMs), style = MaterialTheme.typography.labelSmall, color = SecondaryTxt)
                }

                Spacer(Modifier.height(8.dp))

                // ── Transport controls ────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { player.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint               = if (state.shuffleEnabled) Red else SecondaryTxt,
                        )
                    }
                    IconButton(onClick = { player.skipPrevious() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous",
                            modifier = Modifier.size(32.dp))
                    }
                    Box(
                        modifier         = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { player.playPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint               = Color.Black,
                            modifier           = Modifier.size(36.dp),
                        )
                    }
                    IconButton(onClick = { player.skipNext() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next",
                            modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { player.cycleRepeat() }) {
                        Icon(
                            imageVector = when (state.repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Outlined.RepeatOne
                                else                  -> Icons.Outlined.Repeat
                            },
                            contentDescription = "Repeat",
                            tint               = if (state.repeatMode != Player.REPEAT_MODE_OFF) Red else SecondaryTxt,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Speed + Sleep chips ───────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val currentSpeed = speeds.getOrElse(speedIndex) { 1f }
                    AssistChip(
                        onClick = {
                            speedIndex = (speedIndex + 1) % speeds.size
                            player.setSpeed(speeds[speedIndex])
                        },
                        label = { Text("${currentSpeed}×", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        },
                    )

                    // Bug 6 fix: BedtimeOff replaced with Timer (guaranteed to exist)
                    AssistChip(
                        onClick = { showSleepDialog = true },
                        label   = {
                            Text(
                                text     = if (sleepMinutes > 0) "Sleep ${sleepMinutes}m" else "Sleep Timer",
                                fontSize = 13.sp,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        },
                        colors = if (sleepMinutes > 0)
                            AssistChipDefaults.assistChipColors(leadingIconContentColor = Red)
                        else AssistChipDefaults.assistChipColors(),
                    )
                }

            } else {
                // ── Queue view ────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Queue",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.queue) { idx, queueSong ->
                        val isCurrent = idx == state.currentIndex
                        ListItem(
                            headlineContent   = {
                                Text(
                                    queueSong.title,
                                    color    = if (isCurrent) Red else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(queueSong.artist, color = SecondaryTxt,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            trailingContent = if (isCurrent) {{
                                Icon(Icons.Default.VolumeUp, contentDescription = "Playing", tint = Red)
                            }} else null,
                            modifier = Modifier.clickable { player.playSongs(state.queue, idx) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Sleep Timer") },
        text = {
            Column {
                options.forEach { mins ->
                    ListItem(
                        headlineContent = { Text("$mins minutes") },
                        modifier        = Modifier.clickable { onSet(mins) },
                    )
                }
                ListItem(
                    headlineContent = { Text("End of current song") },
                    modifier        = Modifier.clickable { onSet(1) },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
