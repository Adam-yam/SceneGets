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
import com.adamyam.scenegets.data.MemberBirthdays
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
    // 배경(반투명 카드색)과 텍스트 콘텐츠를 형제 레이어로 분리한다. 배경이 있는 컨테이너
    // 안에 텍스트를 자식으로 바로 넣으면, 일부 기기에서 Glance가 cornerRadius + 반투명
    // 배경을 RemoteViews로 변환할 때 그 안의 텍스트까지 같은 비트맵으로 합쳐 알파를
    // 같이 적용해버려 투명도를 낮출수록 글자까지 옅어지는 문제가 있었다. (WidgetCard와
    // 동일한 원인/해결 방식 — WidgetChrome.kt 주석 참고)
    Box(modifier = GlanceModifier.fillMaxSize()) {
        // 배경 레이어 — 내용 없음
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.surface)
                .cornerRadius(26.dp)
        ) {}

        // 콘텐츠 레이어 — 항상 완전 불투명
        Column(modifier = GlanceModifier.fillMaxSize()) {
            ScheduleHeader()

            when (state) {
                is WidgetState.Loading -> WidgetCenterMessage("스케줄을 불러오는 중...")
                is WidgetState.Failed -> WidgetCenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
                is WidgetState.Loaded -> {
                    val events = futureOnly(MemberBirthdays.mergeInto(state.data))
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
            ScheduleItem(event)
            // Glance/RemoteViews에서는 Row에 준 padding(bottom)이 View의 내부 padding으로
            // 처리되어 background가 그 영역까지 그대로 덮어버린다(=마진처럼 동작하지 않음).
            // 그래서 알약 사이 실제 간격은 배경이 없는 별도의 Spacer로 만들어야
            // 다음 알약과 붙어 보이는(겹치는) 문제가 확실히 사라진다.
            if (index != group.events.lastIndex) {
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ScheduleItem(event: ScheduleEvent) {
    val color = typeColor(event.type)
    val hasTime = event.time.isNotBlank()
    // 시간이 있으면 시간줄 + 제목줄 2줄, 없으면 제목 1줄만 그려지므로
    // 알약의 실제 높이 기준이 되는 컬러 바 높이도 그에 맞춰 줄여준다.
    val barHeight = if (hasTime) 42.dp else 30.dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
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

private fun weekdayShort(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "월"
    2 -> "화"
    3 -> "수"
    4 -> "목"
    5 -> "금"
    6 -> "토"
    else -> "일"
}

@Composable
private fun weekdayColor(date: LocalDate) = when (date.dayOfWeek.value) {
    6 -> WidgetColors.down
    7 -> WidgetColors.up
    else -> WidgetColors.textSecondary
}

@Composable
private fun typeColor(type: String) = when (type) {
    "concert", "fansign", "event" -> WidgetColors.pink
    "broadcast" -> WidgetColors.down
    "radio" -> WidgetColors.accent
    "birthday" -> WidgetColors.accent
    "notice" -> WidgetColors.textSecondary
    else -> WidgetColors.textSecondary
}

// 오늘 이전(과거) 일정은 위젯에서 숨기고, 오늘을 포함한 이후 일정만 남긴다.
// 날짜 파싱에 실패하는 항목은 표시 여부를 판단할 수 없으므로 안전하게 그대로 둔다.
private fun futureOnly(events: List<ScheduleEvent>): List<ScheduleEvent> {
    val today = LocalDate.now()
    return events.filter { event ->
        val date = runCatching { LocalDate.parse(event.date) }.getOrNull()
        date == null || !date.isBefore(today)
    }
}
