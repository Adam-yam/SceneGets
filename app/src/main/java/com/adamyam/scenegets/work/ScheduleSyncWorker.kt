package com.adamyam.scenegets.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.adamyam.scenegets.data.ScheduleRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.schedule.ScheduleWidget

class ScheduleSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = ScheduleRepository(applicationContext).refresh()
        if ((state as? WidgetState.Loaded)?.contentUnchanged != true) {
            ScheduleWidget().updateAll(applicationContext)
        }
        return if (state is WidgetState.Failed) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC = "schedule_sync_periodic"
        const val UNIQUE_ONE_TIME = "schedule_sync_manual"
    }
}
