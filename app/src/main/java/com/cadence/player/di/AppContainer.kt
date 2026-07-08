package com.cadence.player.di

import android.content.Context
import com.cadence.player.data.ArtworkCache
import com.cadence.player.data.FavoritesStorage
import com.cadence.player.data.MediaStoreScanner
import com.cadence.player.data.MusicRepository
import com.cadence.player.data.PlaylistStorage
import com.cadence.player.data.RecentlyPlayedStorage
import com.cadence.player.data.SettingsStorage
import com.cadence.player.playback.PlayerController

class AppContainer(context: Context) {
    val artworkCache         = ArtworkCache(context)
    val settingsStorage      = SettingsStorage(context)
    private val scanner      = MediaStoreScanner(context)
    private val playlists    = PlaylistStorage(context)
    private val favorites    = FavoritesStorage(context)
    private val recentPlayed = RecentlyPlayedStorage(context)
    val repository           = MusicRepository(context, scanner, playlists, favorites, recentPlayed)
    val playerController     = PlayerController(context, repository)
}
