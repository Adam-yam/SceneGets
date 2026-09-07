package com.adamyam.scenegets.widget.common

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class WidgetThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromStorageValue(value: String?): WidgetThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class WidgetAppearance(
    val themeMode: WidgetThemeMode = WidgetThemeMode.SYSTEM,
    val opacityPercent: Int = DEFAULT_OPACITY_PERCENT
) {
    companion object {
        const val DEFAULT_OPACITY_PERCENT = 100
        const val MIN_OPACITY_PERCENT = 0
        const val MAX_OPACITY_PERCENT = 100
    }
}

object WidgetAppearanceKeys {
    val themeMode = stringPreferencesKey("appearance_theme_mode")
    val opacityPercent = intPreferencesKey("appearance_opacity_percent")
}
