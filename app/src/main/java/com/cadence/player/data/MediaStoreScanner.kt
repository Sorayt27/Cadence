package com.cadence.player.data

import android.content.Context
import android.provider.MediaStore
import com.cadence.player.model.Album
import com.cadence.player.model.Artist
import com.cadence.player.model.Song

class MediaStoreScanner(private val context: Context) {

    /**
     * Returns all music on the device. Throws [SecurityException] if the caller
     * hasn't been granted READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE — callers must
     * catch this and handle it gracefully (usually by showing the permission UI).
     */
    fun scanSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durCol)
                if (duration < 5_000) continue
                songs += Song(
                    id          = cursor.getLong(idCol),
                    title       = cursor.getString(titleCol)  ?: "Unknown",
                    artist      = cursor.getString(artistCol) ?: "Unknown Artist",
                    album       = cursor.getString(albumCol)  ?: "Unknown Album",
                    albumId     = cursor.getLong(albumIdCol),
                    duration    = duration,
                    path        = cursor.getString(dataCol)   ?: "",
                    trackNumber = cursor.getInt(trackCol),
                    year        = cursor.getInt(yearCol),
                    dateAdded   = cursor.getLong(dateCol),
                )
            }
        }
        return songs
    }

    fun buildAlbums(songs: List<Song>): List<Album> =
        songs.groupBy { it.albumId }
            .map { (albumId, group) ->
                val first = group.first()
                Album(
                    id        = albumId,
                    title     = first.album,
                    artist    = first.artist,
                    songCount = group.size,
                    year      = group.maxOf { it.year },
                )
            }
            .sortedBy { it.title }

    fun buildArtists(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist }
            .map { (name, group) ->
                Artist(
                    id         = name.hashCode().toLong(),
                    name       = name,
                    albumCount = group.map { it.albumId }.distinct().size,
                    songCount  = group.size,
                )
            }
            .sortedBy { it.name }
}
