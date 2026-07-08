package com.cadence.player.data

import android.content.Context
import com.cadence.player.model.Album
import com.cadence.player.model.Artist
import com.cadence.player.model.Playlist
import com.cadence.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val scanner: MediaStoreScanner,
    private val playlistStorage: PlaylistStorage,
    private val favoritesStorage: FavoritesStorage,
    private val recentlyPlayedStorage: RecentlyPlayedStorage,
) {
    private val _songs     = MutableStateFlow<List<Song>>(emptyList())
    private val _albums    = MutableStateFlow<List<Album>>(emptyList())
    private val _artists   = MutableStateFlow<List<Artist>>(emptyList())
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    private val _recent    = MutableStateFlow<List<Long>>(emptyList())

    val songs:     StateFlow<List<Song>>     = _songs.asStateFlow()
    val albums:    StateFlow<List<Album>>    = _albums.asStateFlow()
    val artists:   StateFlow<List<Artist>>   = _artists.asStateFlow()
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    val favorites: StateFlow<Set<Long>>      = _favorites.asStateFlow()
    val recent:    StateFlow<List<Long>>     = _recent.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val favs  = favoritesStorage.load()
        val songs = scanner.scanSongs().map { it.copy(isFavorite = it.id in favs) }
        _songs.value     = songs
        _albums.value    = scanner.buildAlbums(songs)
        _artists.value   = scanner.buildArtists(songs)
        _playlists.value = playlistStorage.load()
        _favorites.value = favs
        _recent.value    = recentlyPlayedStorage.load()
    }

    // ── Lookups ───────────────────────────────────────────────────────────
    fun songById(id: Long)         = _songs.value.find     { it.id == id }
    fun albumById(id: Long)        = _albums.value.find    { it.id == id }
    fun artistByName(name: String) = _artists.value.find   { it.name == name }
    fun playlistById(id: String)   = _playlists.value.find { it.id == id }

    fun songsForAlbum(albumId: Long) =
        _songs.value.filter { it.albumId == albumId }
            .sortedWith(compareBy({ it.trackNumber }, { it.title }))

    fun songsForArtist(name: String) =
        _songs.value.filter { it.artist == name }.sortedBy { it.title }

    fun songsForPlaylist(playlist: Playlist) = playlist.songIds.mapNotNull { songById(it) }

    fun recentlyAdded()  = _songs.value.sortedByDescending { it.dateAdded }.take(25)
    fun recentlyPlayed() = _recent.value.mapNotNull { songById(it) }
    fun favoriteSongs()  = _songs.value.filter { it.isFavorite }

    fun search(query: String): Triple<List<Song>, List<Album>, List<Artist>> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return Triple(emptyList(), emptyList(), emptyList())
        return Triple(
            _songs.value.filter   { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) },
            _albums.value.filter  { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) },
            _artists.value.filter { it.name.lowercase().contains(q) },
        )
    }

    // ── Mutations ─────────────────────────────────────────────────────────
    fun toggleFavorite(songId: Long) {
        val added = favoritesStorage.toggle(songId)
        val favs  = favoritesStorage.load()
        _favorites.value = favs
        _songs.value = _songs.value.map {
            if (it.id == songId) it.copy(isFavorite = added) else it
        }
    }

    fun recordPlayed(songId: Long) {
        recentlyPlayedStorage.record(songId)
        _recent.value = recentlyPlayedStorage.load()
    }

    // Playlist operations
    fun createPlaylist(name: String)               { playlistStorage.create(name);        reload() }
    fun renamePlaylist(id: String, n: String)       { playlistStorage.rename(id, n);       reload() }
    fun deletePlaylist(id: String)                  { playlistStorage.delete(id);          reload() }
    fun addSongToPlaylist(pId: String, sId: Long)   { playlistStorage.addSong(pId, sId);   reload() }
    fun removeSongFromPlaylist(pId: String, sId: Long){ playlistStorage.removeSong(pId, sId); reload() }

    // Settings actions
    fun clearRecentlyPlayed() {
        recentlyPlayedStorage.clear()
        _recent.value = emptyList()
    }

    fun clearFavorites() {
        favoritesStorage.save(emptySet())
        _favorites.value = emptySet()
        _songs.value = _songs.value.map { it.copy(isFavorite = false) }
    }

    private fun reload() { _playlists.value = playlistStorage.load() }
}
