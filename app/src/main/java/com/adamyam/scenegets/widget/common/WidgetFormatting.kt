package com.adamyam.scenegets.widget.common

import com.adamyam.scenegets.data.WidgetState
private val HIDDEN_TAG_REGEX = Regex("""[(（]\s*리센느\s*[)）]""")
private val EXTRA_SPACE_REGEX = Regex("""\s{2,}""")

fun stripHiddenTags(text: String): String =
    text.replace(HIDDEN_TAG_REGEX, "")
        .replace(EXTRA_SPACE_REGEX, " ")
        .trim()

fun freshnessLabel(state: WidgetState<*>): String = when (state) {
    is WidgetState.Loading -> "불러오는 중..."
    is WidgetState.Failed -> "갱신 실패"
    is WidgetState.Loaded -> {
        val minutes = ((System.currentTimeMillis() - state.fetchedAt) / 60_000L).coerceAtLeast(0)
        val timeText = when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            else -> "${minutes / 60}시간 전"
        }
        if (state.isStale) "갱신 실패 · $timeText" else timeText
    }
}
