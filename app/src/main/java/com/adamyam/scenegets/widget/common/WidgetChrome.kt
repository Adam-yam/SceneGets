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
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.stroke)
                .cornerRadius(OUTER_RADIUS)
        ) {}
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(BORDER_WIDTH)
                .background(WidgetColors.surface)
                .cornerRadius(INNER_RADIUS)
        ) {}
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
