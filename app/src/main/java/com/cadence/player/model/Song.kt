package com.cadence.player.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,       // milliseconds
    val path: String,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long,      // epoch seconds from MediaStore
    val isFavorite: Boolean = false,
)

/** Format milliseconds as m:ss */
fun Song.formattedDuration(): String {
    val totalSec = duration / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
