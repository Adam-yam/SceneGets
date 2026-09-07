package com.adamyam.scenegets.data

import java.time.LocalDate

/**
 * SCENE-FLIX는 스케줄을 반기 단위 파일로 나눠서 올림: schedule_{연도}_h{1|2}.json
 * (예: 2026년 하반기 -> schedule_2026_h2.json, 2027년 상반기 -> schedule_2027_h1.json)
 *
 * 여기서는 오늘 날짜로부터 파일명을 "계산"하기 때문에, 연도가 바뀌어도
 * 코드 수정 없이 자동으로 다음 반기 파일명을 만들어낸다.
 */
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
}
