package com.adamyam.scenegets.widget.common

import androidx.glance.unit.ColorProvider
import com.adamyam.scenegets.R

/**
 * 인앱 디자인(scenegets_app_design.html)의 라이트/다크 토큰과 통일한 위젯 팔레트.
 *
 * 이전 버전은 ColorProvider(day = Color, night = Color) 2-인자 오버로드를 썼는데,
 * 이 프로젝트가 쓰는 Glance 1.1.1에는 그 오버로드가 없어서(ColorProvider(Color) /
 * ColorProvider(resId: Int) 두 가지만 존재) 빌드가 깨졌었다(compileReleaseKotlin 실패).
 *
 * Glance 1.1.1에서 실제로 라이트/다크를 전환하는 정식 방법은 색상을 리소스로 정의하고
 * ColorProvider(R.color.xxx)로 참조하는 것이다. res/values/widget_palette.xml(라이트)과
 * res/values-night/widget_palette.xml(다크)에 같은 이름의 색을 정의해두면 시스템 다크모드에
 * 따라 리소스 qualifier가 알아서 전환된다.
 */
object WidgetColors {
    // 위젯을 감싸는 카드 배경 = 인앱 --surface
    val surface = ColorProvider(R.color.widget_surface)
    // 칩/썸네일 빈자리 등에 쓰는 옅은 배경 = 인앱 --surface-2
    val surfaceVariant = ColorProvider(R.color.widget_surface_variant)
    // 카드 테두리(헤어라인) = 인앱 --stroke
    val stroke = ColorProvider(R.color.widget_stroke)
    // 리스트 항목 사이 구분선 = 인앱 --divider
    val divider = ColorProvider(R.color.widget_divider)

    val textPrimary = ColorProvider(R.color.widget_text_primary)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val textFaint = ColorProvider(R.color.widget_text_faint)

    // 인앱 --accent (보라). 시간 칩, 새로고침 아이콘 등 포인트 컬러.
    val accent = ColorProvider(R.color.widget_accent)
    val accentChipBackground = ColorProvider(R.color.widget_accent_chip_bg)

    // 순위 상승/하락 (한국 차트 관례상 상승=빨강, 하락=파랑) = 인앱 --up/--down
    val up = ColorProvider(R.color.widget_up)
    val down = ColorProvider(R.color.widget_down)
    val flat = ColorProvider(R.color.widget_flat)

    // 이벤트/공연 배지 등에 쓰는 핑크 = 인앱 --pink
    val pink = ColorProvider(R.color.widget_pink)
}
