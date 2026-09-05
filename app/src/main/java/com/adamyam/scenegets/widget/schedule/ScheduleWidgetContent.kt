package com.adamyam.scenegets.widget.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.WidgetCenterMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun ScheduleWidgetContent(state: WidgetState<List<ScheduleEvent>>) {
    // TXT 시안의 단일 화이트 캘린더 카드 구조를 그대로 위젯 내부에 적용한다.
    // 데이터/갱신 로직은 기존 ScheduleRepository와 RefreshScheduleAction을 그대로 사용한다.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .cornerRadius(26.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            ScheduleHeader(state)

            when (state) {
                is WidgetState.Loading -> WidgetCenterMessage("스케줄을 불러오는 중...")
                is WidgetState.Failed -> WidgetCenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
                is WidgetState.Loaded -> {
                    val events = state.data
                    if (events.isEmpty()) {
                        WidgetCenterMessage("예정된 일정이 없어요")
                    } else {
                        val groups = groupEventsByDate(events)
                        Box(modifier = GlanceModifier.defaultWeight()) {
                            LazyColumn(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                            ) {
                                items(groups) { group ->
                                    ScheduleDateCard(group)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ScheduleHeader(state: WidgetState<List<ScheduleEvent>>) {
    val month = when (state) {
        is WidgetState.Loaded -> state.data.firstOrNull()?.date?.let(::monthLabel)
        else -> null
    } ?: monthLabel(LocalDate.now().toString())

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "일정 카드",
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = month,
                    style = TextStyle(
                        color = WidgetColors.textSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            // 기존 위젯의 수동 새로고침 동작은 유지한다.
            Box(
                modifier = GlanceModifier
                    .width(24.dp)
                    .height(24.dp)
                    .cornerRadius(12.dp)
                    .background(WidgetColors.surfaceVariant)
                    .clickable(actionRunCallback<RefreshScheduleAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = WidgetColors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

private data class DateEventGroup(
    val date: String,
    val events: List<ScheduleEvent>
)

private fun groupEventsByDate(events: List<ScheduleEvent>): List<DateEventGroup> =
    events.groupBy { it.date }
        .toSortedMap()
        .map { (date, dayEvents) ->
            DateEventGroup(
                date,
                dayEvents.sortedWith(compareBy({ it.time.isBlank() }, { it.time }))
            )
        }

@Composable
private fun ScheduleDateCard(group: DateEventGroup) {
    val parsed = try {
        LocalDate.parse(group.date)
    } catch (_: DateTimeParseException) {
        null
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(WidgetColors.surfaceVariant)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        DateSide(parsed, group.date)

        Spacer(modifier = GlanceModifier.width(12.dp))
        DashedDivider()
        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            group.events.forEachIndexed { index, event ->
                if (index > 0) Spacer(modifier = GlanceModifier.height(8.dp))
                ScheduleItem(event)
            }
        }
    }
}

@Composable
private fun DateSide(parsed: LocalDate?, rawDate: String) {
    val dateNumber = parsed?.dayOfMonth?.toString() ?: rawDate.takeLast(2)
    val weekday = parsed?.let { weekdayLabel(it) } ?: ""

    Column(
        modifier = GlanceModifier.width(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dateNumber,
            style = TextStyle(
                color = WidgetColors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = weekday,
            style = TextStyle(
                color = WidgetColors.textSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun DashedDivider() {
    // Glance에는 CSS border-left:dashed가 없어서 짧은 선을 반복해 점선을 만든다.
    Column(
        modifier = GlanceModifier.width(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(8) {
            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(3.dp)
                    .background(WidgetColors.divider)
            ) {}
            if (it < 7) Spacer(modifier = GlanceModifier.height(3.dp))
        }
    }
}

@Composable
private fun ScheduleItem(event: ScheduleEvent) {
    val color = typeColor(event.type)

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = timeText(event),
            maxLines = 1,
            style = TextStyle(
                color = color,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = event.title,
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun timeText(event: ScheduleEvent): String =
    if (event.time.isBlank()) "종일" else event.time

private fun typeColor(type: String) = when (type) {
    "concert", "fansign", "event" -> WidgetColors.pink
    "broadcast" -> WidgetColors.down
    "radio" -> WidgetColors.accent
    "notice" -> WidgetColors.textSecondary
    else -> WidgetColors.textSecondary
}

private fun weekdayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "월요일"
    2 -> "화요일"
    3 -> "수요일"
    4 -> "목요일"
    5 -> "금요일"
    6 -> "토요일"
    else -> "일요일"
}

private fun monthLabel(dateString: String): String {
    val parsed = try {
        LocalDate.parse(dateString)
    } catch (_: DateTimeParseException) {
        return dateString
    }
    return String.format(Locale.KOREA, "%d년 %d월", parsed.year, parsed.monthValue)
}
