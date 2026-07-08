package com.cadence.player.data

import android.content.Context
import org.json.JSONArray
import java.io.File

class FavoritesStorage(private val context: Context) {
    private val file: File get() = File(context.filesDir, "favorites.json")

    fun load(): Set<Long> {
        if (!file.exists()) return emptySet()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { arr.getLong(it) }.toSet()
        } catch (_: Exception) { emptySet() }
    }

    fun save(ids: Set<Long>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        file.writeText(arr.toString())
    }

    /** Returns true if the song was *added* (false if removed). */
    fun toggle(id: Long): Boolean {
        val favs  = load().toMutableSet()
        val added = if (id in favs) { favs.remove(id); false } else { favs.add(id); true }
        save(favs)
        return added
    }
}

class RecentlyPlayedStorage(private val context: Context) {
    private val file: File get() = File(context.filesDir, "recently_played.json")
    private val maxSize = 50

    fun load(): List<Long> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { arr.getLong(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun record(songId: Long) {
        val recent = load().toMutableList().also {
            it.remove(songId)
            it.add(0, songId)
            if (it.size > maxSize) it.subList(maxSize, it.size).clear()
        }
        val arr = JSONArray()
        recent.forEach { arr.put(it) }
        file.writeText(arr.toString())
    }

    fun clear() { file.delete() }
}
