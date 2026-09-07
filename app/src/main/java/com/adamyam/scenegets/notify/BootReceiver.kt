package com.adamyam.scenegets.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adamyam.scenegets.data.MemberBirthdays
import com.adamyam.scenegets.data.ScheduleRepository
import com.adamyam.scenegets.data.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = ScheduleRepository(appContext).cachedOrLoading()
                val events = MemberBirthdays.mergeInto((state as? WidgetState.Loaded)?.data ?: emptyList())
                ScheduleNotificationManager.rescheduleAll(appContext, events)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
