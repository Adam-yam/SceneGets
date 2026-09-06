package com.adamyam.scenegets.widget.common

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 위젯 구성(configure) 화면에서 사용자가 고르는 두 가지 설정.
 * - themeMode: 위젯을 시스템 다크모드와 무관하게 강제로 라이트/다크로 고정할 수 있게 한다.
 * - opacityPercent: 위젯 카드 배경(테두리+배경색)의 불투명도. 텍스트/아이콘/칩 배경 등
 *   내부 요소는 항상 100% 불투명 상태를 유지하고, 오직 바깥 카드 배경만 이 값을 따른다.
 */
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

/** Glance의 PreferencesGlanceStateDefinition(위젯별 DataStore)에 저장할 때 쓰는 키. */
object WidgetAppearanceKeys {
    val themeMode = stringPreferencesKey("appearance_theme_mode")
    val opacityPercent = intPreferencesKey("appearance_opacity_percent")
}
