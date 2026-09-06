package com.adamyam.scenegets.widget.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adamyam.scenegets.R

/**
 * 3개 위젯(차트/뉴스/스케줄)이 공유하는 시각 언어.
 * - 인앱 카드(.card)와 통일감을 주기 위해 흰 배경 + 헤어라인 테두리로 "떠 있는 카드" 느낌을 낸다.
 * - Glance 1.1.1에는 border 모디파이어가 없어서, 바깥 Box(테두리색 배경) 안에
 *   1dp 패딩을 준 안쪽 Box(카드색 배경)를 겹치는 방식으로 얇은 테두리를 흉내낸다.
 */
private val OUTER_RADIUS = 20.dp
private val INNER_RADIUS = 19.dp
private val BORDER_WIDTH = 1.dp

@Composable
fun WidgetCard(content: @Composable ColumnScope.() -> Unit) {
    // 배경(테두리+카드색)과 실제 콘텐츠(텍스트)를 같은 Box/Column의 부모-자식이 아니라
    // 완전히 분리된 형제(sibling) 레이어로 쌓는다.
    //
    // 이전에는 배경이 있는 Column 안에 텍스트를 자식으로 바로 넣었는데, cornerRadius +
    // 런타임에 계산되는(=컴파일 타임에 알 수 없는) 반투명 ColorProvider 조합은 일부
    // 기기(특히 API 31 미만)에서 Glance가 RemoteViews로 변환할 때 배경과 그 안의 자식
    // 뷰까지 통째로 하나의 비트맵으로 합성해버린다. 그 결과 카드 배경 알파가 낮아질수록
    // 안에 있는 글자까지 같이 옅어지는 문제가 생겼다.
    //
    // 배경 전용 Box(내용 없음)를 텍스트 Column과 완전히 분리된 형제로 두면, 알파가 적용된
    // 비트맵 합성이 일어나더라도 그 대상은 "내용이 없는 배경 사각형"뿐이라 텍스트는
    // 항상 별도 레이어에서 100% 불투명하게 그려진다.
    Box(modifier = GlanceModifier.fillMaxSize()) {
        // 테두리 레이어 (바깥 배경) — 내용 없음
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.stroke)
                .cornerRadius(OUTER_RADIUS)
        ) {}
        // 카드 배경 레이어 — 내용 없음
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(BORDER_WIDTH)
                .background(WidgetColors.surface)
                .cornerRadius(INNER_RADIUS)
        ) {}
        // 콘텐츠 레이어 — 배경/투명도와 무관하게 항상 완전 불투명
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(BORDER_WIDTH)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content
        )
    }
}

/**
 * 위젯 공통 헤더. 제목과 최신 시각, 새로고침 버튼만 간결하게 배치한다.
 */
@Composable
fun WidgetHeader(
    title: String,
    accentColor: ColorProvider,
    freshness: String,
    refreshAction: Action
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = freshness,
            style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        WidgetRefreshButton(refreshAction)
    }
}

/** 아이콘 탭 영역을 24dp로 넓히고 옅은 배경을 깔아 탭하기 쉬운 원형 버튼으로 만든다. */
@Composable
private fun WidgetRefreshButton(action: Action) {
    Box(
        modifier = GlanceModifier
            .size(24.dp)
            .cornerRadius(12.dp)
            .background(WidgetColors.surfaceVariant)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier.size(13.dp)
        )
    }
}

@Composable
fun WidgetDivider() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WidgetColors.divider)
    ) {}
}

/** 로딩/실패/빈 상태를 헤더 바로 아래가 아니라 남은 영역 정중앙에 표시한다. */
@Composable
fun WidgetCenterMessage(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        )
    }
}
