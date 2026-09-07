package com.adamyam.scenegets.work

import java.util.Calendar

object RefreshAlignment {

    fun millisUntilNext(intervalHours: Int, targetMinute: Int, targetSecond: Int = 0): Long {
        val now = Calendar.getInstance()
        val candidate = now.clone() as Calendar
        candidate.set(Calendar.MINUTE, targetMinute)
        candidate.set(Calendar.SECOND, targetSecond)
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
