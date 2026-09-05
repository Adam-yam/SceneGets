package com.adamyam.scenegets.widget.common

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * 인앱 디자인(scenegets_app_design.html)의 라이트/다크 토큰과 통일한 위젯 팔레트.
 * ColorProvider(day, night)로 시스템 라이트/다크 모드에 맞춰 자동 전환된다.
 */
object WidgetColors {
    // 여러 카드를 담는 위젯의 바탕면 = 인앱 --bg
    val pageBackground = ColorProvider(Color(0xFFF2F2F7))
    // 위젯 전체를 감싸는 카드 배경 = 인앱 --surface
    val surface = ColorProvider(Color(0xFFFFFFFF))
    // 칩/썸네일 빈자리 등에 쓰는 옅은 배경 = 인앱 --surface-2
    val surfaceVariant = ColorProvider(Color(0xFFF2F2F7))
    // 리스트 항목 사이 구분선 = 인앱 --divider
    val divider = ColorProvider(Color(0x223C3C43))

    val textPrimary = ColorProvider(Color(0xFF1C1C1E))
    val textSecondary = ColorProvider(Color(0xFF6C6C70))
    val textFaint = ColorProvider(Color(0xFFAEAEB2))

    // 인앱 --accent (보라). 시간 칩, 새로고침 아이콘 등 포인트 컬러.
    val accent = ColorProvider(Color(0xFF5E5CE6))
    val accentChipBackground = ColorProvider(Color(0x1E5E5CE6))

    // 순위 상승/하락 (한국 차트 관례상 상승=빨강, 하락=파랑) = 인앱 --up/--down
    val up = ColorProvider(Color(0xFFFF3B30))
    val down = ColorProvider(Color(0xFF007AFF))
    val flat = ColorProvider(Color(0xFF8E8E93))

    // 이벤트/공연 배지 등에 쓰는 핑크 = 인앱 --pink
    val pink = ColorProvider(Color(0xFFFF2D55))
    val pinkTint = ColorProvider(Color(0x1FFF2D55))
    val downTint = ColorProvider(Color(0x1F007AFF))
}
