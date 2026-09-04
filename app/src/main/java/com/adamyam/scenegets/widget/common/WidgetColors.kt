package com.adamyam.scenegets.widget.common

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/** SCENE-FLIX 웹의 다크 테마(#0a0a0a 배경)와 톤을 맞춘 위젯 팔레트 */
object WidgetColors {
    val background = ColorProvider(Color(0xFF0A0A0A))
    val cardBackground = ColorProvider(Color(0xFF161616))
    val chipBackground = ColorProvider(Color(0xFF262626))
    val divider = ColorProvider(Color(0xFF2A2A2A))

    val textPrimary = ColorProvider(Color(0xFFFFFFFF))
    val textSecondary = ColorProvider(Color(0xFFA0A0A0))
    val textFaint = ColorProvider(Color(0xFF6E6E6E))

    val up = ColorProvider(Color(0xFFFF5C5C))     // 순위 상승
    val down = ColorProvider(Color(0xFF5C9BFF))   // 순위 하락
    val flat = ColorProvider(Color(0xFF8C8C8C))   // 변동 없음 / NEW
}
