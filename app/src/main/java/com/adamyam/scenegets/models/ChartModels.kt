package com.adamyam.scenegets.models

import kotlinx.serialization.Serializable

@Serializable
data class ChartResponse(
    val updatedAt: String = "",
    val platforms: List<String> = emptyList(),
    val songs: List<ChartSong> = emptyList()
)

@Serializable
data class ChartSong(
    val songName: String,
    val artistName: String,
    val albumImageUrl: String? = null,
    val ranks: Map<String, ChartRank> = emptyMap()
)

@Serializable
data class ChartRank(
    val rank: Int,
    val previousRank: Int? = null
)
