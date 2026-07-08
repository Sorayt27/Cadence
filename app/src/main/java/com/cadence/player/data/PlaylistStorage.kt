package com.cadence.player.data

import android.content.Context
import com.cadence.player.model.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PlaylistStorage(private val context: Context) {

    private val file: File get() = File(context.filesDir, "playlists.json")

    fun load(): List<Playlist> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ids = o.getJSONArray("songIds")
                Playlist(
                    id        = o.getString("id"),
                    name      = o.getString("name"),
                    createdAt = o.getLong("createdAt"),
                    songIds   = (0 until ids.length()).map { ids.getLong(it) },
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun save(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("createdAt", p.createdAt)
            val ids = JSONArray()
            p.songIds.forEach { ids.put(it) }
            o.put("songIds", ids)
            arr.put(o)
        }
        file.writeText(arr.toString())
    }

    fun create(name: String): Playlist {
        val playlists = load().toMutableList()
        val p = Playlist(
            id        = UUID.randomUUID().toString(),
            name      = name,
            songIds   = emptyList(),
            createdAt = System.currentTimeMillis(),
        )
        playlists += p
        save(playlists)
        return p
    }

    fun rename(id: String, newName: String) =
        save(load().map { if (it.id == id) it.copy(name = newName) else it })

    fun delete(id: String) =
        save(load().filter { it.id != id })

    fun addSong(playlistId: String, songId: Long) =
        save(load().map { p ->
            if (p.id == playlistId && songId !in p.songIds)
                p.copy(songIds = p.songIds + songId)
            else p
        })

    fun removeSong(playlistId: String, songId: Long) =
        save(load().map { p ->
            if (p.id == playlistId)
                p.copy(songIds = p.songIds.filter { it != songId })
            else p
        })
}
