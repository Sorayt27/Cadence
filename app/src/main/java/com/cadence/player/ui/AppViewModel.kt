package com.cadence.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.player.MusicApplication
import com.cadence.player.data.MusicRepository
import com.cadence.player.playback.PlayerController
import kotlinx.coroutines.launch

/**
 * Shared ViewModel — does NOT auto-refresh on init because storage permission
 * may not be granted yet. MainActivity calls refresh() once permission is confirmed.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container  get() = (getApplication<MusicApplication>()).container

    val repository: MusicRepository get() = container.repository
    val player: PlayerController     get() = container.playerController

    /** Call this after storage permission is confirmed. */
    fun refresh() = viewModelScope.launch {
        try {
            repository.refresh()
        } catch (e: SecurityException) {
            // Permission revoked at runtime — ignore, UI will stay empty.
        }
    }
}
