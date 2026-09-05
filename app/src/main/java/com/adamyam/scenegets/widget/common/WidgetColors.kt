package com.adamyam.scenegets.widget.common

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * 인앱 디자인(scenegets_app_design.html)의 라이트/다크 토큰과 통일한 위젯 팔레트.
 * ColorProvider(day, night)로 시스템 라이트/다크 모드에 맞춰 자동 전환된다.
 */
object WidgetColors {
    // 이전 버전은 ColorProvider(Color)를 단일 인자로만 호출해서 실제로는
    // 라이트 값 하나로 고정되어 있었다(다크모드에서 전혀 전환되지 않는 버그).
    // ColorProvider(day = ..., night = ...) 2-인자 형태로 인앱 CSS의
    // 라이트/다크 토큰을 각각 그대로 매핑해서 실제 다크모드 전환이 되도록 했다.

    // 여러 카드를 담는 위젯의 바탕면 = 인앱 --bg
    val pageBackground = ColorProvider(day = Color(0xFFF2F2F7), night = Color(0xFF000000))
    // 위젯을 감싸는 카드 배경 = 인앱 --surface
    val surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1C1C1E))
    // 칩/썸네일 빈자리 등에 쓰는 옅은 배경 = 인앱 --surface-2
    val surfaceVariant = ColorProvider(day = Color(0xFFF2F2F7), night = Color(0xFF2C2C2E))
    // 카드 테두리(헤어라인) = 인앱 --stroke
    val stroke = ColorProvider(day = Color(0x1A3C3C43), night = Color(0x12FFFFFF))
    // 리스트 항목 사이 구분선 = 인앱 --divider
    val divider = ColorProvider(day = Color(0x213C3C43), night = Color(0x17FFFFFF))

    val textPrimary = ColorProvider(day = Color(0xFF1C1C1E), night = Color(0xFFFFFFFF))
    val textSecondary = ColorProvider(day = Color(0xFF6C6C70), night = Color(0xFF98989D))
    val textFaint = ColorProvider(day = Color(0xFFAEAEB2), night = Color(0xFF636366))

    // 인앱 --accent (보라). 시간 칩, 새로고침 아이콘 등 포인트 컬러.
    val accent = ColorProvider(day = Color(0xFF5E5CE6), night = Color(0xFF7D7AFF))
    val accentChipBackground = ColorProvider(day = Color(0x1F5E5CE6), night = Color(0x2E7D7AFF))

    // 순위 상승/하락 (한국 차트 관례상 상승=빨강, 하락=파랑) = 인앱 --up/--down
    val up = ColorProvider(day = Color(0xFFFF3B30), night = Color(0xFFFF453A))
    val down = ColorProvider(day = Color(0xFF007AFF), night = Color(0xFF0A84FF))
    val flat = ColorProvider(day = Color(0xFF8E8E93), night = Color(0xFF8E8E93))

    // 이벤트/공연 배지 등에 쓰는 핑크 = 인앱 --pink
    val pink = ColorProvider(day = Color(0xFFFF2D55), night = Color(0xFFFF375F))
    val pinkTint = ColorProvider(day = Color(0x1FFF2D55), night = Color(0x2EFF375F))
    val downTint = ColorProvider(day = Color(0x1F007AFF), night = Color(0x2E0A84FF))
}
