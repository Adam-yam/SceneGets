package com.adamyam.scenegets.widget.schedule

import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.widget.common.WidgetCard
import com.adamyam.scenegets.widget.common.WidgetCenterMessage
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.WidgetDivider
import com.adamyam.scenegets.widget.common.WidgetHeader
import com.adamyam.scenegets.widget.common.freshnessLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun ScheduleWidgetContent(state: WidgetState<List<ScheduleEvent>>) {
    WidgetCard {
        WidgetHeader(
            title = "스케줄",
            accentColor = WidgetColors.accent,
            freshness = freshnessLabel(state),
            refreshAction = actionRunCallback<RefreshScheduleAction>()
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        when (state) {
            is WidgetState.Loading -> WidgetCenterMessage("스케줄을 불러오는 중...")
            is WidgetState.Failed -> WidgetCenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val events = state.data
                if (events.isEmpty()) {
                    WidgetCenterMessage("예정된 일정이 없어요")
                } else {
                    // 같은 날짜는 시간이 달라도 하나의 그룹으로 합쳐서 보여준다.
                    val groups = groupEventsByDate(events)
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(groups) { group -> EventGroupSection(group) }
                    }
                }
            }
        }
    }
}

/** 같은 날짜(yyyy-MM-dd)의 일정을 하나로 묶은 그룹. 시간이 달라도 날짜가 같으면 한 그룹에 표시한다. */
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

// 날짜 헤더의 요일 표기. 날짜 숫자만으로는 무슨 요일인지 바로 안 와닿아서
// "월요일"처럼 완전한 형태로 붙여 날짜 헤더 한 줄만 봐도 바로 알 수 있게 했다.
private fun weekdayShortLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "월요일"
    DayOfWeek.TUESDAY -> "화요일"
    DayOfWeek.WEDNESDAY -> "수요일"
    DayOfWeek.THURSDAY -> "목요일"
    DayOfWeek.FRIDAY -> "금요일"
    DayOfWeek.SATURDAY -> "토요일"
    DayOfWeek.SUNDAY -> "일요일"
}

@Composable
private fun EventGroupSection(group: DateEventGroup) {
    // 탭 액션 없음 - 정보 표시 전용.
    // 카드형 배경 대신 여백과 얇은 구분선만으로 그룹을 나눠서 한 화면에
    // 더 많은 일정이 가볍게 들어오도록 했다.
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        DateHeader(group.date)
        Spacer(modifier = GlanceModifier.height(8.dp))
        group.events.forEachIndexed { index, event ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.height(9.dp))
            }
            EventLine(event)
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
    WidgetDivider()
    Spacer(modifier = GlanceModifier.height(10.dp))
}

/**
 * 날짜를 한눈에 알아보게 굵고 크게 키우고, 요일은 주말이면 색으로 구분한다.
 * 오늘/내일은 날짜 옆에 작은 강조색 알약(pill) 태그로 따로 붙여서
 * 날짜 숫자 자체는 항상 같은 자리·같은 크기로 훑을 수 있게 했다.
 */
@Composable
private fun DateHeader(date: String) {
    val parsed = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }
    if (parsed == null) {
        Text(
            text = date,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
        return
    }

    val today = LocalDate.now()
    val isToday = parsed == today
    val isTomorrow = parsed == today.plusDays(1)
    val weekdayColor = when (parsed.dayOfWeek) {
        DayOfWeek.SATURDAY -> WidgetColors.down
        DayOfWeek.SUNDAY -> WidgetColors.up
        else -> WidgetColors.textSecondary
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${parsed.monthValue}.${parsed.dayOfMonth}",
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = weekdayShortLabel(parsed.dayOfWeek),
            style = TextStyle(color = weekdayColor, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        if (isToday || isTomorrow) {
            DateTag(if (isToday) "오늘" else "내일")
        }
    }
}

@Composable
private fun DateTag(label: String) {
    Box(
        modifier = GlanceModifier
            .background(WidgetColors.accentChipBackground)
            .cornerRadius(20.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(color = WidgetColors.accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * 시간을 맨 앞 고정폭 칸에 둬서, 여러 일정이 쌓여도 시간이 세로로 열을
 * 맞춰 보이게 했다(표처럼 훑을 수 있음). 시간 미정 일정은 "종일"로 표시.
 */
@Composable
private fun EventLine(event: ScheduleEvent) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.width(34.dp)) {
            if (event.time.isNotBlank()) {
                Text(
                    text = event.time,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = "종일",
                    style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(6.dp))
        TypeDot(event.type)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = event.title,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun TypeDot(type: String) {
    Box(
        modifier = GlanceModifier
            .size(7.dp)
            .cornerRadius(3.5.dp)
            .background(typeColor(type))
    ) {}
}

private fun typeColor(type: String) = when (type) {
    "broadcast" -> WidgetColors.down
    "radio" -> WidgetColors.textSecondary
    "event" -> WidgetColors.pink
    "fansign" -> WidgetColors.accent
    "concert" -> WidgetColors.pink
    "notice" -> WidgetColors.textSecondary
    else -> WidgetColors.textSecondary
}
