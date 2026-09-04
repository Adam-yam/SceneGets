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

// 날짜 헤더에 쓰는 요일 전체 이름 (블록 대신 텍스트 헤더로 표시할 때 사용)
private fun weekdayFullLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "월요일"
    DayOfWeek.TUESDAY -> "화요일"
    DayOfWeek.WEDNESDAY -> "수요일"
    DayOfWeek.THURSDAY -> "목요일"
    DayOfWeek.FRIDAY -> "금요일"
    DayOfWeek.SATURDAY -> "토요일"
    DayOfWeek.SUNDAY -> "일요일"
}

@Composable
private fun EventGroupCard(group: DateEventGroup) {
    // 탭 액션 없음 - 정보 표시 전용
    // 이전에는 왼쪽에 날짜를 박스(블록)로 분리해서 보여줬는데, 카드 안에 또 다른
    // 박스가 겹치는 느낌이라 답답해 보였다. 대신 날짜는 카드 "바깥" 위쪽에
    // 배경 없는 얇은 텍스트 헤더로만 표시하고(월/일 + 요일, 주말은 색으로 강조),
    // 카드 내부는 내용(타입+제목)과 시간에만 집중하도록 정리했다.
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        DateHeader(group.date)
        Spacer(modifier = GlanceModifier.height(6.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetColors.cardBackground)
                .cornerRadius(14.dp)
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            group.events.forEachIndexed { index, event ->
                if (index > 0) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WidgetColors.divider)
                    ) {}
                    Spacer(modifier = GlanceModifier.height(10.dp))
                }
                EventDetailLine(event)
            }
        }
    }
    Spacer(modifier = GlanceModifier.height(10.dp))
}

@Composable
private fun DateHeader(date: String) {
    // 배경 박스 없이 순수 텍스트로만 "몇 월 며칠 + 요일"을 보여준다.
    // 토요일/일요일은 요일 텍스트 색을 강조색으로 바꿔서 구분한다.
    val parsed = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (parsed != null) {
            Text(
                text = "${parsed.monthValue}월 ${parsed.dayOfMonth}일",
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            val weekdayColor = when (parsed.dayOfWeek) {
                DayOfWeek.SATURDAY -> WidgetColors.down
                DayOfWeek.SUNDAY -> WidgetColors.up
                else -> WidgetColors.textSecondary
            }
            Text(
                text = weekdayFullLabel(parsed.dayOfWeek),
                style = TextStyle(color = weekdayColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        } else {
            Text(
                text = date,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun EventDetailLine(event: ScheduleEvent) {
    // 내용(타입 배지 + 제목)은 왼쪽에서 세로로 쌓아 크고 굵게, 시간은 오른쪽에
    // 독립된 색상 칩으로 분리했다. 시간이 없는 일정은 칩 자체를 그리지 않아서
    // "미정" 같은 불필요한 텍스트 없이 내용 컬럼이 자연스럽게 폭을 채운다.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            TypeBadge(event.type)
            Spacer(modifier = GlanceModifier.height(5.dp))
            Text(
                text = event.title,
                maxLines = 2,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (event.time.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            TimeChip(event.time)
        }
    }
}

@Composable
private fun TimeChip(time: String) {
    Box(
        modifier = GlanceModifier
            .background(WidgetColors.accentChipBackground)
            .cornerRadius(8.dp)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            text = time,
            style = TextStyle(
                color = WidgetColors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun TypeBadge(type: String) {
    Row(
        modifier = GlanceModifier
            .background(typeColor(type))
            .cornerRadius(6.dp)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = typeLabel(type),
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
