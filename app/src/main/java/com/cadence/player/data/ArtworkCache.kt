package com.cadence.player.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache

class ArtworkCache(private val context: Context) {

    private val cache = LruCache<Long, Bitmap?>(80)

    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId,
        )

    /** Blocking load — call from IO dispatcher. Returns null when no art exists. */
    fun load(albumId: Long): Bitmap? {
        val cached = cache.get(albumId)
        if (cached != null) return cached
        return try {
            context.contentResolver.openInputStream(albumArtUri(albumId))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }.also { cache.put(albumId, it) }
    }
}
