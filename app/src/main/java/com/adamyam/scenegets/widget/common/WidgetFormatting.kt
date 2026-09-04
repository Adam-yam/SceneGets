package com.adamyam.scenegets.widget.common

import com.adamyam.scenegets.data.WidgetState

// 곡명/아티스트명에 원본 데이터 특성상 섞여 들어오는 "(리센느)" 표기는
// 위젯에는 노출하지 않는다 (데이터 자체는 건드리지 않고 표시할 때만 제거).
private val HIDDEN_TAG_REGEX = Regex("""[(（]\s*리센느\s*[)）]""")
private val EXTRA_SPACE_REGEX = Regex("""\s{2,}""")

fun stripHiddenTags(text: String): String =
    text.replace(HIDDEN_TAG_REGEX, "")
        .replace(EXTRA_SPACE_REGEX, " ")
        .trim()

/**
 * 위젯 헤더에 "N분 전" 같은 신선도 표기를 만든다.
 * isStale인 경우(=갱신 실패했지만 캐시로 대체) "갱신 실패 · N분 전"처럼 실패도 함께 드러낸다.
 * -> 예전에 겪은 silent failure 문제를 UI에서도 반복하지 않기 위함.
 */
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
