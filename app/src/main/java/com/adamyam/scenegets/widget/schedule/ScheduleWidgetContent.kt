package com.adamyam.scenegets.widget.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import com.adamyam.scenegets.R
import java.time.LocalDate
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
            ScheduleHeader()

            when (state) {
                is WidgetState.Loading -> WidgetCenterMessage("스케줄을 불러오는 중...")
                is WidgetState.Failed -> WidgetCenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
                is WidgetState.Loaded -> {
                    val events = futureOnly(withBirthdays(state.data))
                    if (events.isEmpty()) {
                        WidgetCenterMessage("예정된 일정이 없어요")
                    } else {
                        val groups = groupEventsByDate(events)
                        Box(modifier = GlanceModifier.fillMaxWidth()) {
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
private fun ScheduleHeader() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "일정",
            style = TextStyle(
                color = WidgetColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Box(
            modifier = GlanceModifier
                .width(24.dp)
                .height(24.dp)
                .cornerRadius(12.dp)
                .background(WidgetColors.surfaceVariant)
                .clickable(actionRunCallback<RefreshScheduleAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "새로고침",
                modifier = GlanceModifier
                    .width(13.dp)
                    .height(13.dp)
            )
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

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // Fantastical 스타일의 날짜 라벨: 날짜별 일정 묶음의 시작점에 고정된 듯한
        // 가벼운 헤더를 두어 카드가 많아져도 날짜를 빠르게 찾을 수 있게 한다.
        Row(
            modifier = GlanceModifier.padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (parsed != null) {
                Text(
                    text = String.format(Locale.KOREA, "%d월 %d일 · ", parsed.monthValue, parsed.dayOfMonth),
                    style = TextStyle(
                        color = WidgetColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = weekdayShort(parsed) + "요일",
                    style = TextStyle(
                        color = weekdayColor(parsed),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                Text(
                    text = group.date,
                    style = TextStyle(
                        color = WidgetColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        group.events.forEachIndexed { index, event ->
            ScheduleItem(event, addBottomPadding = index != group.events.lastIndex)
        }
    }
}

@Composable
private fun ScheduleItem(event: ScheduleEvent, addBottomPadding: Boolean = true) {
    val color = typeColor(event.type)
    val hasTime = event.time.isNotBlank()
    // 시간이 있으면 시간줄 + 제목줄 2줄, 없으면 제목 1줄만 그려지므로
    // 알약의 실제 높이 기준이 되는 컬러 바 높이도 그에 맞춰 줄여준다.
    val barHeight = if (hasTime) 42.dp else 30.dp

    Row(
        // 알약 사이 여백은 background/cornerRadius보다 먼저(바깥쪽에) 적용해야
        // 여백이 알약 색 바깥의 실제 간격이 된다. 반대로 두면 여백이 알약 안쪽으로
        // 흡수돼 다음 알약과 그대로 맞붙어(겹쳐) 보이게 된다.
        modifier = GlanceModifier
            .fillMaxWidth()
            .then(if (addBottomPadding) GlanceModifier.padding(bottom = 6.dp) else GlanceModifier)
            .background(WidgetColors.surfaceVariant)
            .cornerRadius(12.dp)
            .padding(end = 10.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(barHeight)
                .background(color)
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = if (hasTime) 8.dp else 6.dp)
        ) {
            if (hasTime) {
                Text(
                    text = event.time,
                    maxLines = 1,
                    style = TextStyle(
                        color = color,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = event.title,
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private fun formatDateLabel(parsed: LocalDate?, rawDate: String): String {
    if (parsed == null) return rawDate
    return String.format(
        Locale.KOREA,
        "%d월 %d일 · %s요일",
        parsed.monthValue,
        parsed.dayOfMonth,
        weekdayShort(parsed)
    )
}

private fun weekdayShort(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "월"
    2 -> "화"
    3 -> "수"
    4 -> "목"
    5 -> "금"
    6 -> "토"
    else -> "일"
}

private fun weekdayColor(date: LocalDate) = when (date.dayOfWeek.value) {
    6 -> WidgetColors.down
    7 -> WidgetColors.up
    else -> WidgetColors.textSecondary
}

private fun typeColor(type: String) = when (type) {
    "concert", "fansign", "event" -> WidgetColors.pink
    "broadcast" -> WidgetColors.down
    "radio" -> WidgetColors.accent
    "birthday" -> WidgetColors.accent
    "notice" -> WidgetColors.textSecondary
    else -> WidgetColors.textSecondary
}

private data class Birthday(val month: Int, val day: Int, val name: String)

private val birthdays = listOf(
    Birthday(5, 25, "원이"),
    Birthday(10, 11, "리브"),
    Birthday(11, 29, "미나미"),
    Birthday(8, 19, "메이"),
    Birthday(11, 27, "제나")
)

// 오늘 이전(과거) 일정은 위젯에서 숨기고, 오늘을 포함한 이후 일정만 남긴다.
// 날짜 파싱에 실패하는 항목은 표시 여부를 판단할 수 없으므로 안전하게 그대로 둔다.
private fun futureOnly(events: List<ScheduleEvent>): List<ScheduleEvent> {
    val today = LocalDate.now()
    return events.filter { event ->
        val date = runCatching { LocalDate.parse(event.date) }.getOrNull()
        date == null || !date.isBefore(today)
    }
}

private fun withBirthdays(events: List<ScheduleEvent>): List<ScheduleEvent> {
    val years = (events.mapNotNull { it.date.takeIf { d -> d.length >= 4 }?.take(4)?.toIntOrNull() } + LocalDate.now().year).toSet()
    val existingBirthdayDates = events.filter { it.type == "birthday" }.map { it.date }.toSet()
    val birthdayEvents = years.flatMap { year ->
        birthdays.mapNotNull { birthday ->
            val date = runCatching { LocalDate.of(year, birthday.month, birthday.day) }.getOrNull() ?: return@mapNotNull null
            if (date.toString() in existingBirthdayDates) null
            else ScheduleEvent(
                date = date.toString(),
                title = "${birthday.name} 생일",
                type = "birthday",
                source = "local"
            )
        }
    }
    return events + birthdayEvents
}
