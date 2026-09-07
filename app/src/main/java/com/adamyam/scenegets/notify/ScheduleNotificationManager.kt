package com.adamyam.scenegets.notify

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.adamyam.scenegets.MainActivity
import com.adamyam.scenegets.R
import com.adamyam.scenegets.models.ScheduleEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object ScheduleNotificationManager {
    const val MODE_ONCE = "once"
    const val MODE_REPEAT = "repeat"
    const val CHANNEL_ID = "schedule_reminders"

    private const val PREFS_NAME = "scenegets_notifications"
    private const val KEY_MODE = "mode"
    private const val KEY_LEAD_HOURS = "lead_hours"
    private const val KEY_ENABLED_EVENTS = "enabled_events"
    private const val DEFAULT_LEAD_HOURS = 1
    private const val MAX_LEAD_HOURS = 5

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "일정 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "RESCENE 스케줄 시작 전 알림"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun getMode(context: Context): String = prefs(context).getString(KEY_MODE, MODE_ONCE) ?: MODE_ONCE

    fun getLeadHours(context: Context): Int = prefs(context).getInt(KEY_LEAD_HOURS, DEFAULT_LEAD_HOURS)

    fun setSettings(context: Context, mode: String, leadHours: Int) {
        val safeMode = if (mode == MODE_REPEAT) MODE_REPEAT else MODE_ONCE
        val safeHours = leadHours.coerceIn(1, MAX_LEAD_HOURS)
        prefs(context).edit().putString(KEY_MODE, safeMode).putInt(KEY_LEAD_HOURS, safeHours).apply()
    }

    private fun eventKey(date: String, time: String, title: String): String = "$date|$time|$title"

    fun isEnabled(context: Context, date: String, time: String, title: String): Boolean =
        loadEnabledMap(context).has(eventKey(date, time, title))

    private fun loadEnabledMap(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_ENABLED_EVENTS, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun saveEnabledMap(context: Context, map: JSONObject) {
        prefs(context).edit().putString(KEY_ENABLED_EVENTS, map.toString()).apply()
    }

    fun setEventEnabled(
        context: Context,
        date: String,
        time: String,
        title: String,
        enabled: Boolean
    ): Boolean {
        if (enabled && eventStartMillis(date, time) == null) return false
        val key = eventKey(date, time, title)
        val map = loadEnabledMap(context)
        if (enabled) {
            val entry = JSONObject()
            entry.put("date", date)
            entry.put("time", time)
            entry.put("title", title)
            map.put(key, entry)
        } else {
            map.remove(key)
        }
        saveEnabledMap(context, map)
        if (enabled) {
            scheduleEvent(context, date, time, title)
        } else {
            cancelEvent(context, date, time, title)
        }
        return enabled
    }

    private fun eventStartMillis(date: String, time: String): Long? {
        if (time.isBlank()) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        return runCatching { fmt.parse("$date $time")?.time }.getOrNull()
    }

    private fun requestCode(key: String, offsetHours: Int): Int = key.hashCode() * 31 + offsetHours

    private fun alarmIntent(
        context: Context,
        date: String,
        time: String,
        title: String,
        offsetHours: Int
    ): PendingIntent {
        val key = eventKey(date, time, title)
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ScheduleAlarmReceiver.EXTRA_OFFSET_HOURS, offsetHours)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(key, offsetHours),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleEvent(context: Context, date: String, time: String, title: String) {
        val startMillis = eventStartMillis(date, time) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val mode = getMode(context)
        val leadHours = getLeadHours(context)
        val offsets = if (mode == MODE_REPEAT) (1..leadHours).toList() else listOf(leadHours)
        offsets.forEach { hours ->
            val triggerAt = startMillis - hours * 3_600_000L
            if (triggerAt <= System.currentTimeMillis()) return@forEach
            val pi = alarmIntent(context, date, time, title, hours)
            runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi) }
        }
    }

    private fun cancelEvent(context: Context, date: String, time: String, title: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (hours in 1..MAX_LEAD_HOURS) {
            alarmManager.cancel(alarmIntent(context, date, time, title, hours))
        }
    }

    fun rescheduleAll(context: Context, events: List<ScheduleEvent>) {
        val map = loadEnabledMap(context)
        val eventKeys = events.map { eventKey(it.date, it.time, it.title) }.toSet()
        val updated = JSONObject()
        val keys = map.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = map.getJSONObject(key)
            val date = entry.optString("date")
            val time = entry.optString("time")
            val title = entry.optString("title")
            cancelEvent(context, date, time, title)
            val startMillis = eventStartMillis(date, time)
            if (eventKeys.contains(key) && startMillis != null && startMillis > System.currentTimeMillis()) {
                scheduleEvent(context, date, time, title)
                updated.put(key, entry)
            }
        }
        saveEnabledMap(context, updated)
    }

    fun buildNotification(context: Context, title: String, offsetHours: Int): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, "schedule")
        }
        val pi = PendingIntent.getActivity(
            context,
            title.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (offsetHours <= 0) "곧 시작해요" else "${offsetHours}시간 후 시작해요"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
