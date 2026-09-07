package com.adamyam.scenegets.widget.common

import androidx.compose.runtime.Composable
import androidx.glance.unit.ColorProvider

/**
 * 인앱 디자인(scenegets_app_design.html)의 라이트/다크 토큰과 통일한 위젯 팔레트.
 *
 * 예전에는 리소스(qualifier)로만 라이트/다크를 정의해 시스템 다크모드만 따라갔지만,
 * 위젯 구성(configure) 화면에서 사용자가 라이트/다크를 강제로 고르고 배경 투명도까지
 * 조절할 수 있어야 해서, 실제 값은 WidgetTheme.kt의 CompositionLocal(LocalWidgetScheme /
 * LocalWidgetOpacity)에서 가져온다. 각 위젯의 Widget.kt가 WidgetThemedContent { ... }로
 * 감싸주면, 여기 있는 프로퍼티들은 그 안에서 계산된 값을 그대로 읽어온다.
 *
 * surface / stroke(=카드 바깥 배경+테두리)에만 사용자가 고른 투명도를 곱해서 적용하고,
 * 그 외(텍스트, 칩 배경 등)는 항상 완전 불투명 상태를 유지한다 — "배경만 투명, 글자는 그대로".
 */
object WidgetColors {
    val surface: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.surface.copy(alpha = LocalWidgetOpacity.current))
    val stroke: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.stroke.copy(alpha = LocalWidgetOpacity.current))

    // 칩/썸네일 빈자리 등에 쓰는 옅은 배경. 카드 배경과 달리 항상 불투명 — 위젯을 투명하게
    // 만들어도 순위 칩 등 내부 요소는 또렷하게 읽혀야 하기 때문.
    val surfaceVariant: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.surfaceVariant)

    val divider: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.divider)

    val textPrimary: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.textPrimary)
    val textSecondary: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.textSecondary)
    val textFaint: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.textFaint)

    val accent: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.accent)
    val accentChipBackground: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.accentChipBackground)

    val up: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.up)
    val down: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.down)
    val flat: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.flat)

    val pink: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.pink)
}
