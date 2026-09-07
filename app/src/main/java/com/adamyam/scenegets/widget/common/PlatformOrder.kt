package com.adamyam.scenegets.widget.common

import com.adamyam.scenegets.models.ChartRank

/** 차트 위젯에서 플랫폼 칩을 항상 같은 순서로 보여주기 위한 고정 순서 + 한글 라벨 */
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

    /** 실제 순위가 존재하는 플랫폼만, 고정된 순서로 정렬해서 반환 */
    fun sort(ranks: Map<String, ChartRank>): List<Pair<String, ChartRank>> =
        order.filter { ranks.containsKey(it) }.map { it to ranks.getValue(it) } +
            // 혹시 위 목록에 없는 새 플랫폼이 추가되더라도 누락되지 않게 뒤에 붙여줌
            ranks.keys.filterNot { order.contains(it) }.map { it to ranks.getValue(it) }

    fun label(platform: String): String = labels[platform] ?: platform
}
