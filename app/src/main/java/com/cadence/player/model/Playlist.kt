package com.cadence.player.model

data class Playlist(
    val id: String,           // UUID
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long,      // epoch ms
)
