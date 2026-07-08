package com.cadence.player.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SongSortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date Added"),
    DURATION("Duration"),
}

enum class AlbumSortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    YEAR("Year"),
}

data class AppSettings(
    val crossfadeEnabled: Boolean = false,
    val crossfadeDuration: Int = 3,          // seconds 1–12
    val soundCheckEnabled: Boolean = false,
    val songSortOrder: SongSortOrder = SongSortOrder.TITLE,
    val albumSortOrder: AlbumSortOrder = AlbumSortOrder.TITLE,
    val showAlbumArtInNotification: Boolean = true,
    val defaultPlaybackSpeed: Float = 1f,
)

class SettingsStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cadence_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        crossfadeEnabled            = prefs.getBoolean("crossfade_enabled", false),
        crossfadeDuration           = prefs.getInt("crossfade_duration", 3),
        soundCheckEnabled           = prefs.getBoolean("sound_check", false),
        songSortOrder               = SongSortOrder.valueOf(
            prefs.getString("song_sort", SongSortOrder.TITLE.name) ?: SongSortOrder.TITLE.name
        ),
        albumSortOrder              = AlbumSortOrder.valueOf(
            prefs.getString("album_sort", AlbumSortOrder.TITLE.name) ?: AlbumSortOrder.TITLE.name
        ),
        showAlbumArtInNotification  = prefs.getBoolean("notif_art", true),
        defaultPlaybackSpeed        = prefs.getFloat("default_speed", 1f),
    )

    fun setCrossfade(enabled: Boolean) {
        prefs.edit().putBoolean("crossfade_enabled", enabled).apply()
        _settings.value = _settings.value.copy(crossfadeEnabled = enabled)
    }

    fun setCrossfadeDuration(seconds: Int) {
        prefs.edit().putInt("crossfade_duration", seconds).apply()
        _settings.value = _settings.value.copy(crossfadeDuration = seconds)
    }

    fun setSoundCheck(enabled: Boolean) {
        prefs.edit().putBoolean("sound_check", enabled).apply()
        _settings.value = _settings.value.copy(soundCheckEnabled = enabled)
    }

    fun setSongSortOrder(order: SongSortOrder) {
        prefs.edit().putString("song_sort", order.name).apply()
        _settings.value = _settings.value.copy(songSortOrder = order)
    }

    fun setAlbumSortOrder(order: AlbumSortOrder) {
        prefs.edit().putString("album_sort", order.name).apply()
        _settings.value = _settings.value.copy(albumSortOrder = order)
    }

    fun setShowAlbumArtInNotification(enabled: Boolean) {
        prefs.edit().putBoolean("notif_art", enabled).apply()
        _settings.value = _settings.value.copy(showAlbumArtInNotification = enabled)
    }

    fun setDefaultSpeed(speed: Float) {
        prefs.edit().putFloat("default_speed", speed).apply()
        _settings.value = _settings.value.copy(defaultPlaybackSpeed = speed)
    }

    fun reset() {
        prefs.edit().clear().apply()
        _settings.value = AppSettings()
    }
}
