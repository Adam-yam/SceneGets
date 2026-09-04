package com.adamyam.scenegets.widget.schedule

import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.R
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.freshnessLabel

@Composable
fun ScheduleWidgetContent(state: WidgetState<List<ScheduleEvent>>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .padding(8.dp)
    ) {
        ScheduleHeader(state)
        Spacer(modifier = GlanceModifier.height(4.dp))

        when (state) {
            is WidgetState.Loading -> CenterMessage("스케줄을 불러오는 중...")
            is WidgetState.Failed -> CenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val events = state.data
                if (events.isEmpty()) {
                    CenterMessage("예정된 일정이 없어요")
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(events) { event -> EventRow(event) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(state: WidgetState<List<ScheduleEvent>>) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "스케줄",
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = freshnessLabel(state),
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 9.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier
                .size(16.dp)
                .clickable(actionRunCallback<RefreshScheduleAction>())
        )
    }
}

@Composable
private fun EventRow(event: ScheduleEvent) {
    // 탭 액션 없음 - 정보 표시 전용
    // 왼쪽 색상 바로 일정 종류를 표시하고, 위에는 종류/시간 같은 메타 정보,
    // 아래에는 제목을 크게 배치해서 한눈에 스캔하기 쉬운 순서로 정리한다.
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(10.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .width(3.dp)
                .height(32.dp)
                .cornerRadius(2.dp)
                .background(typeColor(event.type))
        ) {}
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(event.type)
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = formatDateTime(event),
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 9.sp)
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = event.title,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

@Composable
private fun TypeBadge(type: String) {
    Row(
        modifier = GlanceModifier
            .background(typeColor(type))
            .cornerRadius(5.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = typeLabel(type),
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        )
    }
}

private fun typeLabel(type: String): String = when (type) {
    "broadcast" -> "방송"
    "radio" -> "라디오"
    "event" -> "행사"
    "fansign" -> "팬사인회"
    "concert" -> "공연"
    "notice" -> "공지"
    else -> type.ifBlank { "일정" }
}

private fun typeColor(type: String) = when (type) {
    "broadcast" -> WidgetColors.down
    "radio" -> WidgetColors.flat
    "event" -> WidgetColors.up
    "fansign" -> WidgetColors.chipBackground
    "concert" -> WidgetColors.up
    else -> WidgetColors.chipBackground
}

private fun formatDateTime(event: ScheduleEvent): String {
    val datePart = event.date.replace("-", ".")
    return if (event.time.isBlank()) datePart else "$datePart · ${event.time}"
}

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
