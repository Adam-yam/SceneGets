package com.adamyam.scenegets.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.chart.ChartWidget

class ChartSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = ChartRepository(applicationContext).refresh()
        ChartWidget().updateAll(applicationContext)
        return if (state is WidgetState.Failed) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC = "chart_sync_periodic"
        const val UNIQUE_ONE_TIME = "chart_sync_manual"
    }
}
