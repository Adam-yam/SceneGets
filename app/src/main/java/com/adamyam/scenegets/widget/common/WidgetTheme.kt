package com.adamyam.scenegets.widget.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.glance.currentState

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

internal val LocalWidgetOpacity = staticCompositionLocalOf { 1f }

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
