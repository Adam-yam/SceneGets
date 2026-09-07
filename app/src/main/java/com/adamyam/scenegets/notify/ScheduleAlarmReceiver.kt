package com.adamyam.scenegets.notify

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val offsetHours = intent.getIntExtra(EXTRA_OFFSET_HOURS, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ScheduleNotificationManager.ensureChannel(context)
        val notification = ScheduleNotificationManager.buildNotification(context, title, offsetHours)
        val notificationId = title.hashCode() * 31 + offsetHours
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_OFFSET_HOURS = "extra_offset_hours"
    }
}
