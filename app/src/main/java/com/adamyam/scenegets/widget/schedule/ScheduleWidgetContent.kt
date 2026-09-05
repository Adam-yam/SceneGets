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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun ScheduleWidgetContent(state: WidgetState<List<ScheduleEvent>>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.pageBackground)
            .cornerRadius(20.dp)
            .padding(10.dp)
    ) {
        ScheduleHeader(state)
        Spacer(modifier = GlanceModifier.height(8.dp))

        when (state) {
            is WidgetState.Loading -> CenterMessage("스케줄을 불러오는 중...")
            is WidgetState.Failed -> CenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val events = state.data
                if (events.isEmpty()) {
                    CenterMessage("예정된 일정이 없어요")
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

@Composable
private fun ScheduleHeader(state: WidgetState<List<ScheduleEvent>>) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "스케줄",
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = freshnessLabel(state),
            style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier
                .size(15.dp)
                .clickable(actionRunCallback<RefreshScheduleAction>())
        )
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

// 날짜 헤더용 짧은 요일 표기(월/화/수...). 한눈에 훑을 때 "월요일"보다 부담이 적다.
private fun weekdayShortLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

/**
 * 오늘/내일은 날짜 대신 "오늘"/"내일"로 바로 보여주고, 그 외에는
 * "9.12 (금)"처럼 짧게 표기해서 헤더 한 줄이 눈에 바로 들어오게 한다.
 */
private fun dateHeaderLabel(parsed: LocalDate): String {
    val today = LocalDate.now()
    return when (parsed) {
        today -> "오늘 · ${parsed.monthValue}.${parsed.dayOfMonth}"
        today.plusDays(1) -> "내일 · ${parsed.monthValue}.${parsed.dayOfMonth}"
        else -> "${parsed.monthValue}.${parsed.dayOfMonth} (${weekdayShortLabel(parsed.dayOfWeek)})"
    }
}

@Composable
private fun EventGroupSection(group: DateEventGroup) {
    // 탭 액션 없음 - 정보 표시 전용.
    // 카드형 배경 대신 여백과 얇은 구분선만으로 그룹을 나눠서 한 화면에
    // 더 많은 일정이 가볍게 들어오도록 했다. 날짜 헤더는 색 배지 없이
    // 텍스트만 쓰고, 오늘/내일만 강조색으로 표시해 시선이 자연스럽게 간다.
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        DateHeader(group.date)
        Spacer(modifier = GlanceModifier.height(6.dp))
        group.events.forEachIndexed { index, event ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
            EventLine(event)
        }
    }
    Spacer(modifier = GlanceModifier.height(4.dp))
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WidgetColors.divider)
    ) {}
    Spacer(modifier = GlanceModifier.height(10.dp))
}

@Composable
private fun DateHeader(date: String) {
    val parsed = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }
    if (parsed != null) {
        val today = LocalDate.now()
        val isNearTerm = parsed == today || parsed == today.plusDays(1)
        val color = when {
            isNearTerm -> WidgetColors.accent
            parsed.dayOfWeek == DayOfWeek.SATURDAY -> WidgetColors.down
            parsed.dayOfWeek == DayOfWeek.SUNDAY -> WidgetColors.up
            else -> WidgetColors.textSecondary
        }
        Text(
            text = dateHeaderLabel(parsed),
            style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
    } else {
        Text(
            text = date,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun EventLine(event: ScheduleEvent) {
    // 배지(pill)와 시간 칩을 없애고 타입은 작은 색 점, 시간은 배경 없는 텍스트로
    // 단순화했다. 한 줄에 담기는 요소가 줄어들어 리스트를 훑기가 더 가볍다.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        TypeDot(event.type)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = event.title,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
        )
        if (event.time.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = event.time,
                style = TextStyle(color = WidgetColors.textFaint, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            )
        }
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

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
