package com.cadence.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cadence.player.data.MusicRepository
import com.cadence.player.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val playbackSpeed: Float = 1f,
)

class PlayerController(
    private val context: Context,
    private val repository: MusicRepository,
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = syncState()
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            syncState()
            item?.mediaId?.toLongOrNull()?.let { repository.recordPlayed(it) }
        }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = syncState()
        override fun onRepeatModeChanged(mode: Int) = syncState()
        override fun onPlaybackStateChanged(state: Int) = syncState()
        override fun onPlaybackParametersChanged(params: PlaybackParameters) = syncState()
    }

    fun connect() {
        // Guard against double-connect (e.g. activity recreated)
        if (controller != null || controllerFuture != null) return

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                controller = future.get().also {
                    it.addListener(listener)
                    syncState()
                }
            } catch (e: Exception) {
                // Service binding failed — playback unavailable but app won't crash.
                controllerFuture = null
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    // ── Playback commands ──────────────────────────────────────────────────

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipNext()     { controller?.seekToNextMediaItem() }
    fun skipPrevious() {
        val c = controller ?: return
        if (c.currentPosition > 3_000) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else                   -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) { controller?.setPlaybackSpeed(speed) }

    fun playNext(song: Song) {
        val c = controller ?: return
        val pos = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(pos, song.toMediaItem())
        syncState()
    }

    fun addToQueue(song: Song) {
        controller?.addMediaItem(song.toMediaItem())
        syncState()
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
        syncState()
    }

    fun getPosition(): Long = controller?.currentPosition ?: 0L

    // ── Internal ───────────────────────────────────────────────────────────

    private fun Song.toMediaItem() = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(path))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

    private fun syncState() {
        val c = controller ?: return
        val queue = (0 until c.mediaItemCount).mapNotNull { i ->
            c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { repository.songById(it) }
        }
        val idx = c.currentMediaItemIndex
        _state.value = PlaybackState(
            currentSong    = queue.getOrNull(idx),
            isPlaying      = c.isPlaying,
            positionMs     = c.currentPosition.coerceAtLeast(0L),
            durationMs     = c.duration.coerceAtLeast(0L),
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode     = c.repeatMode,
            queue          = queue,
            currentIndex   = idx,
            playbackSpeed  = c.playbackParameters.speed,
        )
    }
}
