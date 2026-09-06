package com.adamyam.scenegets.data

import com.adamyam.scenegets.models.ScheduleEvent
import java.time.LocalDate

/**
 * 서버 스케줄 데이터에는 없는 멤버 생일을 "type=birthday" 가상 일정으로 합성해서 끼워 넣는다.
 *
 * 원래 이 로직은 ScheduleWidgetContent.kt 안에만 있어서 위젯의 "스케줄"에는 생일이
 * 표시되지만, 인앱 스케줄 탭/홈 화면에는 나타나지 않는 불일치가 있었다. 위젯과 인앱이
 * 항상 같은 일정 목록을 보여주도록 이 로직을 공유 위치로 옮겼다.
 */
object MemberBirthdays {

    private data class Birthday(val month: Int, val day: Int, val name: String)

    private val birthdays = listOf(
        Birthday(5, 25, "원이"),
        Birthday(10, 11, "리브"),
        Birthday(11, 29, "미나미"),
        Birthday(8, 19, "메이"),
        Birthday(11, 27, "제나")
    )

    /**
     * [events]에 이미 등장하는 연도(및 올해)를 기준으로 생일 가상 일정을 만들어 덧붙인다.
     * 서버 데이터에 이미 동일 날짜의 birthday 타입 일정이 있으면 중복 추가하지 않는다.
     */
    fun mergeInto(events: List<ScheduleEvent>, today: LocalDate = LocalDate.now()): List<ScheduleEvent> {
        val years = (
            events.mapNotNull { it.date.takeIf { d -> d.length >= 4 }?.take(4)?.toIntOrNull() } +
                today.year
            ).toSet()
        val existingBirthdayDates = events.filter { it.type == "birthday" }.map { it.date }.toSet()
        val birthdayEvents = years.flatMap { year ->
            birthdays.mapNotNull { birthday ->
                val date = runCatching { LocalDate.of(year, birthday.month, birthday.day) }.getOrNull()
                    ?: return@mapNotNull null
                // 이미 지난 생일은 만들지 않는다 (오늘 포함, 이후만).
                if (date.isBefore(today)) return@mapNotNull null
                if (date.toString() in existingBirthdayDates) {
                    null
                } else {
                    ScheduleEvent(
                        date = date.toString(),
                        title = "${birthday.name} 생일",
                        type = "birthday",
                        source = "local"
                    )
                }
            }
        }
        return (events + birthdayEvents).sortedWith(compareBy({ it.date }, { it.time }))
    }
}
