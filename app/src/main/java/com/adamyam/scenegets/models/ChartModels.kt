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
    // 플랫폼별 실제 순위가 있는 항목만 키로 들어있음 (SCENE-FLIX 원본 데이터 특성)
    val ranks: Map<String, ChartRank> = emptyMap()
)

@Serializable
data class ChartRank(
    val rank: Int,
    val previousRank: Int? = null
)
