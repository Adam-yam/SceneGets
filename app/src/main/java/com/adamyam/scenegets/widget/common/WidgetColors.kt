package com.adamyam.scenegets.widget.common

import androidx.compose.runtime.Composable
import androidx.glance.unit.ColorProvider

object WidgetColors {
    val surface: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.surface.copy(alpha = LocalWidgetOpacity.current))
    val stroke: ColorProvider
        @Composable get() = ColorProvider(LocalWidgetScheme.current.stroke.copy(alpha = LocalWidgetOpacity.current))
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
