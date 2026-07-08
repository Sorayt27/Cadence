package com.cadence.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.player.data.AlbumSortOrder
import com.cadence.player.data.SettingsStorage
import com.cadence.player.data.SongSortOrder
import com.cadence.player.ui.theme.Red
import com.cadence.player.ui.theme.SecondaryTxt

@Composable
fun SettingsScreen(
    settingsStorage: SettingsStorage,
    onClearRecentlyPlayed: () -> Unit,
    onClearFavorites: () -> Unit,
) {
    val settings by settingsStorage.settings.collectAsState()

    var showSongSortSheet    by remember { mutableStateOf(false) }
    var showAlbumSortSheet   by remember { mutableStateOf(false) }
    var showSpeedSheet       by remember { mutableStateOf(false) }
    var showResetConfirm     by remember { mutableStateOf(false) }
    var showClearRecent      by remember { mutableStateOf(false) }
    var showClearFavs        by remember { mutableStateOf(false) }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showSongSortSheet) {
        PickerDialog(
            title     = "Sort Songs By",
            options   = SongSortOrder.entries.map { it.label },
            selected  = settings.songSortOrder.label,
            onDismiss = { showSongSortSheet = false },
            onPick    = { idx ->
                settingsStorage.setSongSortOrder(SongSortOrder.entries[idx])
                showSongSortSheet = false
            },
        )
    }

    if (showAlbumSortSheet) {
        PickerDialog(
            title     = "Sort Albums By",
            options   = AlbumSortOrder.entries.map { it.label },
            selected  = settings.albumSortOrder.label,
            onDismiss = { showAlbumSortSheet = false },
            onPick    = { idx ->
                settingsStorage.setAlbumSortOrder(AlbumSortOrder.entries[idx])
                showAlbumSortSheet = false
            },
        )
    }

    if (showSpeedSheet) {
        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        PickerDialog(
            title     = "Default Speed",
            options   = speeds.map { "${it}×" },
            selected  = "${settings.defaultPlaybackSpeed}×",
            onDismiss = { showSpeedSheet = false },
            onPick    = { idx ->
                settingsStorage.setDefaultSpeed(speeds[idx])
                showSpeedSheet = false
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title   = { Text("Reset All Settings") },
            text    = { Text("All settings will return to defaults. Your library, playlists and favorites won't be affected.") },
            confirmButton = {
                TextButton(onClick = { settingsStorage.reset(); showResetConfirm = false }) {
                    Text("Reset", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearRecent) {
        AlertDialog(
            onDismissRequest = { showClearRecent = false },
            title   = { Text("Clear Recently Played") },
            text    = { Text("This will remove all tracks from your Recently Played history.") },
            confirmButton = {
                TextButton(onClick = { onClearRecentlyPlayed(); showClearRecent = false }) {
                    Text("Clear", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { showClearRecent = false }) { Text("Cancel") } },
        )
    }

    if (showClearFavs) {
        AlertDialog(
            onDismissRequest = { showClearFavs = false },
            title   = { Text("Clear Favorites") },
            text    = { Text("This will remove all songs from your Favorites.") },
            confirmButton = {
                TextButton(onClick = { onClearFavorites(); showClearFavs = false }) {
                    Text("Clear", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { showClearFavs = false }) { Text("Cancel") } },
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                text     = "Settings",
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp),
            )
        }

        // ── Playback ────────────────────────────────────────────────────────
        item { SectionLabel("Playback") }

        item {
            SettingsCard {
                ToggleRow(
                    icon    = Icons.Default.Shuffle,
                    title   = "Crossfade Songs",
                    subtitle = "Smoothly blend between tracks",
                    checked = settings.crossfadeEnabled,
                    onToggle = settingsStorage::setCrossfade,
                )

                if (settings.crossfadeEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Duration", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${settings.crossfadeDuration}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Red,
                            )
                        }
                        Slider(
                            value         = settings.crossfadeDuration.toFloat(),
                            onValueChange = { settingsStorage.setCrossfadeDuration(it.toInt()) },
                            valueRange    = 1f..12f,
                            steps         = 10,
                            colors        = SliderDefaults.colors(
                                thumbColor       = Red,
                                activeTrackColor = Red,
                            ),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1s", style = MaterialTheme.typography.labelSmall, color = SecondaryTxt)
                            Text("12s", style = MaterialTheme.typography.labelSmall, color = SecondaryTxt)
                        }
                    }
                }

                SettingsDivider()

                ToggleRow(
                    icon     = Icons.Default.Equalizer,
                    title    = "Sound Check",
                    subtitle = "Automatically adjust volume for a consistent listening level",
                    checked  = settings.soundCheckEnabled,
                    onToggle = settingsStorage::setSoundCheck,
                )

                SettingsDivider()

                NavigateRow(
                    icon     = Icons.Default.Speed,
                    title    = "Default Playback Speed",
                    value    = "${settings.defaultPlaybackSpeed}×",
                    onClick  = { showSpeedSheet = true },
                )

                SettingsDivider()

                ToggleRow(
                    icon     = Icons.Default.Notifications,
                    title    = "Album Art in Notification",
                    subtitle = "Show cover art on the lock screen and in the notification",
                    checked  = settings.showAlbumArtInNotification,
                    onToggle = settingsStorage::setShowAlbumArtInNotification,
                )
            }
        }

        // ── Library ─────────────────────────────────────────────────────────
        item { SectionLabel("Library") }

        item {
            SettingsCard {
                NavigateRow(
                    icon    = Icons.Default.SortByAlpha,
                    title   = "Sort Songs By",
                    value   = settings.songSortOrder.label,
                    onClick = { showSongSortSheet = true },
                )
                SettingsDivider()
                NavigateRow(
                    icon    = Icons.Default.Album,
                    title   = "Sort Albums By",
                    value   = settings.albumSortOrder.label,
                    onClick = { showAlbumSortSheet = true },
                )
            }
        }

        // ── Advanced ────────────────────────────────────────────────────────
        item { SectionLabel("Advanced") }

        item {
            SettingsCard {
                DestructiveRow(
                    icon    = Icons.Default.History,
                    title   = "Clear Recently Played",
                    onClick = { showClearRecent = true },
                )
                SettingsDivider()
                DestructiveRow(
                    icon    = Icons.Default.FavoriteBorder,
                    title   = "Clear Favorites",
                    onClick = { showClearFavs = true },
                )
                SettingsDivider()
                DestructiveRow(
                    icon    = Icons.Default.RestartAlt,
                    title   = "Reset All Settings",
                    onClick = { showResetConfirm = true },
                )
            }
        }

        // ── About ────────────────────────────────────────────────────────────
        item { SectionLabel("About") }

        item {
            SettingsCard {
                InfoRow(
                    icon  = Icons.Default.MusicNote,
                    title = "Cadence",
                    value = "Version 1.0",
                )
                SettingsDivider()
                InfoRow(
                    icon  = Icons.Default.Code,
                    title = "Source Code",
                    value = "github.com/Sorayt27/Cadence",
                )
                SettingsDivider()
                InfoRow(
                    icon  = Icons.Default.Copyright,
                    title = "License",
                    value = "MIT",
                )
            }
        }
    }
}

// ── Reusable row types ─────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Red, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryTxt)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedThumbColor = Red, checkedTrackColor = Red.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun NavigateRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Red, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SecondaryTxt)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryTxt,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DestructiveRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Red, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Red)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SecondaryTxt, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SecondaryTxt)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = SecondaryTxt,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 52.dp),
        color     = MaterialTheme.colorScheme.outline,
        thickness = 0.5.dp,
    )
}

@Composable
private fun PickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = {
            Column {
                options.forEachIndexed { idx, label ->
                    val isSelected = label == selected
                    ListItem(
                        headlineContent = {
                            Text(
                                label,
                                color     = if (isSelected) Red else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingContent = if (isSelected) {{
                            Icon(Icons.Default.Check, contentDescription = null, tint = Red)
                        }} else null,
                        modifier = Modifier.clickable { onPick(idx) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
