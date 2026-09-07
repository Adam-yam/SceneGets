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
import com.adamyam.scenegets.widget.common.WidgetCard
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.WidgetCenterMessage
import com.adamyam.scenegets.R
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun ScheduleWidgetContent(state: WidgetState<List<ScheduleEvent>>) {
    WidgetCard {
        ScheduleHeader()

        when (state) {
            is WidgetState.Loading -> WidgetCenterMessage("스케줄을 불러오는 중...")
            is WidgetState.Failed -> WidgetCenterMessage("스케줄을 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val events = withinTwoMonths(MemberBirthdays.mergeInto(state.data))
                if (events.isEmpty()) {
                    WidgetCenterMessage("예정된 일정이 없어요")
                } else {
                    val groups = groupEventsByDate(events)
                    LazyColumn(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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

@Composable
private fun ScheduleHeader() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
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
private fun withinTwoMonths(events: List<ScheduleEvent>): List<ScheduleEvent> {
    val today = LocalDate.now()
    val cutoff = today.plusMonths(2)
    return events.filter { event ->
        val date = runCatching { LocalDate.parse(event.date) }.getOrNull()
        date == null || (!date.isBefore(today) && !date.isAfter(cutoff))
    }
}
