package com.adamyam.scenegets.widget.common

import com.adamyam.scenegets.models.ChartRank

object PlatformOrder {
    private val order = listOf("melon", "genie", "vibe", "bugs", "flo", "youtube_music", "spotify")

    private val labels = mapOf(
        "melon" to "멜론",
        "genie" to "지니",
        "vibe" to "바이브",
        "bugs" to "벅스",
        "flo" to "플로",
        "youtube_music" to "유튜브뮤직",
        "spotify" to "스포티파이"
    )

    fun sort(ranks: Map<String, ChartRank>): List<Pair<String, ChartRank>> =
        order.filter { ranks.containsKey(it) }.map { it to ranks.getValue(it) } +
            ranks.keys.filterNot { order.contains(it) }.map { it to ranks.getValue(it) }

    fun label(platform: String): String = labels[platform] ?: platform
}
