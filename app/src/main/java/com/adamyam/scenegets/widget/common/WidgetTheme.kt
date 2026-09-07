package com.adamyam.scenegets.widget.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.glance.currentState

/**
 * 인앱 디자인(scenegets_app_design.html)의 :root / prefers-color-scheme(dark) 토큰과
 * 1:1 매핑되는 원본 색상값. res/values(-night)/widget_palette.xml과 같은 값을 그대로 옮겨왔다.
 *
 * 예전에는 이 값들을 리소스(qualifier)로만 갖고 있어서 "시스템 다크모드"만 따라갈 수 있었는데,
 * 위젯 구성 화면에서 사용자가 라이트/다크를 강제로 고를 수 있게 하려면 코드 쪽에서 직접
 * 라이트/다크 세트를 골라 쓸 수 있어야 한다. 그래서 Color 상수로 옮기고, 실제 밝기 판정은
 * WidgetThemedContent에서 (시스템 설정 또는 사용자가 고른 값) 기준으로 계산한다.
 */
internal data class WidgetColorScheme(
    val surface: Color,
    val surfaceVariant: Color,
    val stroke: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textFaint: Color,
    val accent: Color,
    val accentChipBackground: Color,
    val up: Color,
    val down: Color,
    val flat: Color,
    val pink: Color
)

internal object WidgetPalette {
    val Light = WidgetColorScheme(
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF2F2F7),
        stroke = Color(0x1A3C3C43),
        divider = Color(0x213C3C43),
        textPrimary = Color(0xFF1C1C1E),
        textSecondary = Color(0xFF6C6C70),
        textFaint = Color(0xFFAEAEB2),
        accent = Color(0xFF5E5CE6),
        accentChipBackground = Color(0x1F5E5CE6),
        up = Color(0xFFFF3B30),
        down = Color(0xFF007AFF),
        flat = Color(0xFF8E8E93),
        pink = Color(0xFFFF2D55)
    )

    val Dark = WidgetColorScheme(
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        stroke = Color(0x12FFFFFF),
        divider = Color(0x17FFFFFF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF98989D),
        textFaint = Color(0xFF636366),
        accent = Color(0xFF7D7AFF),
        accentChipBackground = Color(0x2E7D7AFF),
        up = Color(0xFFFF453A),
        down = Color(0xFF0A84FF),
        flat = Color(0xFF8E8E93),
        pink = Color(0xFFFF375F)
    )

    fun forDarkMode(isDark: Boolean): WidgetColorScheme = if (isDark) Dark else Light
}

internal val LocalWidgetScheme = staticCompositionLocalOf { WidgetPalette.Light }

/** 1f = 완전 불투명(기본값). WidgetCard의 바깥 배경(카드 배경+테두리)에만 적용된다. */
internal val LocalWidgetOpacity = staticCompositionLocalOf { 1f }

/**
 * 위젯 구성 화면에서 저장한 값(currentState)을 읽어 실제 색 스킴/투명도를 계산하고,
 * 하위 콘텐츠(ChartWidgetContent 등)를 그 값으로 감싼다.
 *
 * "시스템 설정 따르기"를 골랐다면 Glance의 LocalContext로 현재 다크모드 여부를 직접 확인한다
 * (Glance는 Compose UI의 isSystemInDarkTheme()을 쓸 수 없으므로 Configuration을 직접 읽는다).
 */
@Composable
fun WidgetThemedContent(content: @Composable () -> Unit) {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val themeMode = WidgetThemeMode.fromStorageValue(prefs[WidgetAppearanceKeys.themeMode])
    val opacityPercent = (prefs[WidgetAppearanceKeys.opacityPercent] ?: WidgetAppearance.DEFAULT_OPACITY_PERCENT)
        .coerceIn(WidgetAppearance.MIN_OPACITY_PERCENT, WidgetAppearance.MAX_OPACITY_PERCENT)

    val context = LocalContext.current
    val systemIsDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    val isDark = when (themeMode) {
        WidgetThemeMode.SYSTEM -> systemIsDark
        WidgetThemeMode.LIGHT -> false
        WidgetThemeMode.DARK -> true
    }

    CompositionLocalProvider(
        LocalWidgetScheme provides WidgetPalette.forDarkMode(isDark),
        LocalWidgetOpacity provides (opacityPercent / 100f)
    ) {
        content()
    }
}
