package com.adamyam.scenegets.data

import com.adamyam.scenegets.models.ScheduleEvent
import java.time.LocalDate

object MemberBirthdays {

    private data class Birthday(val month: Int, val day: Int, val name: String)

    private val birthdays = listOf(
        Birthday(5, 25, "원이"),
        Birthday(10, 11, "리브"),
        Birthday(11, 29, "미나미"),
        Birthday(8, 19, "메이"),
        Birthday(11, 27, "제나")
    )

    fun mergeInto(events: List<ScheduleEvent>, today: LocalDate = LocalDate.now()): List<ScheduleEvent> {
        val years = setOf(today.year, today.year + 1)
        val existingBirthdayDates = events.filter { it.type == "birthday" }.map { it.date }.toSet()
        val birthdayEvents = years.flatMap { year ->
            birthdays.mapNotNull { birthday ->
                val date = runCatching { LocalDate.of(year, birthday.month, birthday.day) }.getOrNull()
                    ?: return@mapNotNull null
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
