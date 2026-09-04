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
import androidx.glance.unit.ColorProvider
import com.adamyam.scenegets.R
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.freshnessLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
                    // 같은 날짜는 시간이 달라도 하나의 카드로 합쳐서 보여준다.
                    val groups = groupEventsByDate(events)
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(groups) { group -> EventGroupCard(group) }
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

/** 같은 날짜(yyyy-MM-dd)의 일정을 하나로 묶은 그룹. 시간이 달라도 날짜가 같으면 한 카드에 표시한다. */
private data class DateEventGroup(val date: String, val events: List<ScheduleEvent>)

/**
 * 날짜 기준으로 일정을 그룹핑한다. groupBy는 처음 등장한 순서를 유지하므로
 * 원본 목록의 날짜 순서는 그대로 보존되고, 같은 날짜의 일정만 한 그룹으로 묶인다.
 * 그룹 내부는 시간 미정 일정을 맨 뒤로 보내고 나머지는 이른 시간순으로 정렬한다.
 */
private fun groupEventsByDate(events: List<ScheduleEvent>): List<DateEventGroup> =
    events.groupBy { it.date }
        .map { (date, dayEvents) ->
            DateEventGroup(date, dayEvents.sortedWith(compareBy({ it.time.isBlank() }, { it.time })))
        }

@Composable
private fun EventGroupCard(group: DateEventGroup) {
    // 탭 액션 없음 - 정보 표시 전용
    // 날짜/요일은 카드당 한 번만 크고 굵게 배치하고, 그 아래로 같은 날짜의
    // 각 일정(시간/종류/제목)을 줄줄이 나열한다. 일정이 여러 개면 얇은 구분선으로 나눈다.
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
                .background(typeColor(group.events.first().type))
        ) {}
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            DateHeaderLine(group.date)
            Spacer(modifier = GlanceModifier.height(4.dp))
            group.events.forEachIndexed { index, event ->
                if (index > 0) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WidgetColors.divider)
                    ) {}
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                EventDetailLine(event)
            }
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

@Composable
private fun DateHeaderLine(date: String) {
    val weekday = weekdayInfo(date)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = date.replace("-", "."),
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        if (weekday != null) {
            Spacer(modifier = GlanceModifier.width(2.dp))
            Text(
                text = "(${weekday.label})",
                style = TextStyle(color = weekday.color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun EventDetailLine(event: ScheduleEvent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (event.time.isNotBlank()) {
            Text(
                text = event.time,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        TypeBadge(event.type)
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = event.title,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
    }
}

private data class WeekdayInfo(val label: String, val color: ColorProvider)

/**
 * "yyyy-MM-dd" 문자열에서 요일을 계산한다.
 * 토요일은 파란색(WidgetColors.down), 일요일은 빨간색(WidgetColors.up)으로 강조하고,
 * 평일은 기본 보조 색상을 사용한다. 날짜 형식이 예상과 다르면 요일 표시를 생략한다.
 */
private fun weekdayInfo(dateStr: String): WeekdayInfo? {
    val date = try {
        LocalDate.parse(dateStr)
    } catch (e: DateTimeParseException) {
        return null
    }
    val label = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
    val color = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> WidgetColors.down
        DayOfWeek.SUNDAY -> WidgetColors.up
        else -> WidgetColors.textSecondary
    }
    return WeekdayInfo(label, color)
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

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
