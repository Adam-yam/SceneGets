package com.adamyam.scenegets.work

import java.util.Calendar

/**
 * 자동 갱신 시각을 벽시계 기준으로 정렬하기 위한 유틸.
 *
 * 예) intervalHours=1, targetMinute=0  -> 매시 정각(0분)
 *     intervalHours=2, targetMinute=3  -> 짝수 시(0,2,4,...)의 3분
 *
 * WorkManager/AlarmManager는 Doze, 배터리 최적화 등의 영향으로
 * 정확히 그 순간에 실행됨을 보장하지 않는다(특히 배터리 제한이 걸려 있는 경우
 * 지연될 수 있음). 이 함수는 "다음 정렬 시각까지 걸리는 시간"을 계산해
 * 최대한 그 시각에 맞춰 실행되도록 초기 지연(initial delay)을 주기 위한 것.
 */
object RefreshAlignment {

    /** 지금부터 다음 정렬 시각까지 남은 밀리초. */
    fun millisUntilNext(intervalHours: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val candidate = now.clone() as Calendar
        candidate.set(Calendar.MINUTE, targetMinute)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)

        val hourNow = candidate.get(Calendar.HOUR_OF_DAY)
        val alignedHour = (hourNow / intervalHours) * intervalHours
        candidate.set(Calendar.HOUR_OF_DAY, alignedHour)

        if (candidate.timeInMillis <= now.timeInMillis) {
            candidate.add(Calendar.HOUR_OF_DAY, intervalHours)
        }
        return candidate.timeInMillis - now.timeInMillis
    }
}
