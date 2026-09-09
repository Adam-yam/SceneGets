package com.adamyam.scenegets.data

import java.time.LocalDate

object SchedulePeriod {

    data class Period(val year: Int, val half: Int) {
        val fileName: String get() = "schedule_${year}_h${half}.json"
    }

    fun current(today: LocalDate = LocalDate.now()): Period {
        val half = if (today.monthValue <= 6) 1 else 2
        return Period(today.year, half)
    }

    fun next(period: Period): Period =
        if (period.half == 1) Period(period.year, 2) else Period(period.year + 1, 1)

    fun relevantFileNames(today: LocalDate = LocalDate.now()): Set<String> {
        val cur = current(today)
        return setOf(cur.fileName, next(cur).fileName)
    }
}
