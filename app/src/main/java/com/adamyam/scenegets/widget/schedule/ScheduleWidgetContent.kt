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

// 날짜 블록의 고정 폭. EventDetailLine의 시간 칩과 함께 카드 레이아웃의
// 좌/우 "고정 앵커" 역할을 하므로 상수로 빼서 두 곳에서 일관되게 쓴다.
private val DATE_BLOCK_WIDTH = 56.dp

@Composable
private fun EventGroupCard(group: DateEventGroup) {
    // 탭 액션 없음 - 정보 표시 전용
    // 카드를 세 영역으로 명확히 분리했다: 왼쪽 = 날짜(요일+큰 숫자+월),
    // 가운데 = 내용(타입 배지 + 제목, 굵고 크게), 오른쪽 = 시간(색이 들어간
    // 칩으로 강조). 세 정보가 서로 다른 위치/스타일을 가져서 한눈에 훑어도
    // "언제 / 무엇을 / 몇 시에"가 각각 바로 들어오도록 했다.
    // 왼쪽 날짜 블록은 카드 배경과 다른 톤을 줘서 시각적으로도 분리되게 한다.
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(14.dp)
            .padding(vertical = 10.dp, horizontal = 10.dp)
    ) {
        DateBlock(group.date)
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
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
    Spacer(modifier = GlanceModifier.height(8.dp))
}

private data class DateParts(val day: String, val month: String, val weekday: WeekdayInfo?)

/** "yyyy-MM-dd" 문자열을 날짜 블록에 쓸 일/월/요일로 쪼갠다. 파싱 실패 시 원본 문자열을 그대로 보여준다. */
private fun dateParts(dateStr: String): DateParts {
    val weekday = weekdayInfo(dateStr)
    val date = try {
        LocalDate.parse(dateStr)
    } catch (e: DateTimeParseException) {
        null
    }
    return if (date != null) {
        DateParts(
            day = date.dayOfMonth.toString().padStart(2, '0'),
            month = "${date.monthValue}월",
            weekday = weekday
        )
    } else {
        DateParts(day = dateStr.replace("-", "."), month = "", weekday = null)
    }
}

@Composable
private fun DateBlock(date: String) {
    val parts = dateParts(date)
    // 카드 배경(cardBackground)보다 한 톤 밝은 chipBackground로 영역을 분리하고,
    // 날짜 숫자를 24sp까지 키워서 "언제"가 카드 안에서 가장 먼저 눈에 들어오게 한다.
    Column(
        modifier = GlanceModifier
            .width(DATE_BLOCK_WIDTH)
            .background(WidgetColors.chipBackground)
            .cornerRadius(12.dp)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (parts.weekday != null) {
            Text(
                text = parts.weekday.label,
                style = TextStyle(color = parts.weekday.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
        }
        Text(
            text = parts.day,
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
        if (parts.month.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = parts.month,
                style = TextStyle(color = WidgetColors.textFaint, fontSize = 9.sp)
            )
        }
    }
}

@Composable
private fun EventDetailLine(event: ScheduleEvent) {
    // 내용(타입 배지 + 제목)은 왼쪽에서 세로로 쌓아 크고 굵게, 시간은 오른쪽에
    // 독립된 색상 칩으로 분리했다. 제목과 시간이 같은 줄에서 같은 스타일로
    // 경쟁하지 않고, 서로 다른 위치·색으로 "내용"과 "시간"이 각각 바로
    // 눈에 띄도록 한다.
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
        Spacer(modifier = GlanceModifier.width(8.dp))
        TimeChip(event.time)
    }
}

@Composable
private fun TimeChip(time: String) {
    // 시간이 정해지지 않은 일정도 있으므로, 그 경우엔 흐린 톤의 "미정" 칩을 보여줘서
    // 오른쪽 칩 자리가 비어 카드 균형이 깨지지 않게 하면서도 색으로 구분한다.
    val hasTime = time.isNotBlank()
    Box(
        modifier = GlanceModifier
            .background(if (hasTime) WidgetColors.accentChipBackground else WidgetColors.chipBackground)
            .cornerRadius(8.dp)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (hasTime) time else "미정",
            style = TextStyle(
                color = if (hasTime) WidgetColors.accent else WidgetColors.textFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
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
