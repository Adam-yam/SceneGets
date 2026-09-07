package com.adamyam.scenegets.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.adamyam.scenegets.widget.chart.ChartWidgetReceiver
import com.adamyam.scenegets.widget.news.NewsWidgetReceiver
import com.adamyam.scenegets.widget.schedule.ScheduleWidgetReceiver
import java.util.concurrent.TimeUnit

object WidgetWorkScheduler {
    private const val CHART_INTERVAL_HOURS = 1
    private const val NEWS_SCHEDULE_INTERVAL_HOURS = 2
    private const val ALIGN_TARGET_MINUTE = 0
    private const val ALIGN_TARGET_SECOND = 30

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("chart_sync_periodic")
        workManager.cancelUniqueWork("news_sync_periodic")
        workManager.cancelUniqueWork("schedule_sync_periodic")
        if (!hasWidgets(context)) return

        workManager.enqueueUniquePeriodicWork(
            ChartSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ChartSyncWorker>(CHART_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(CHART_INTERVAL_HOURS, ALIGN_TARGET_MINUTE, ALIGN_TARGET_SECOND),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(networkConstraints())
                .build()
        )
        workManager.enqueueUniquePeriodicWork(
            NewsSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NewsSyncWorker>(NEWS_SCHEDULE_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(NEWS_SCHEDULE_INTERVAL_HOURS, ALIGN_TARGET_MINUTE, ALIGN_TARGET_SECOND),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(networkConstraints())
                .build()
        )
        workManager.enqueueUniquePeriodicWork(
            ScheduleSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScheduleSyncWorker>(NEWS_SCHEDULE_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(NEWS_SCHEDULE_INTERVAL_HOURS, ALIGN_TARGET_MINUTE, ALIGN_TARGET_SECOND),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(networkConstraints())
                .build()
        )
    }

    fun cancelIfUnused(context: Context) {
        if (!hasWidgets(context)) cancelAll(context)
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(ChartSyncWorker.UNIQUE_PERIODIC)
        workManager.cancelUniqueWork(NewsSyncWorker.UNIQUE_PERIODIC)
        workManager.cancelUniqueWork(ScheduleSyncWorker.UNIQUE_PERIODIC)
        workManager.cancelUniqueWork(ChartSyncWorker.UNIQUE_ONE_TIME)
        workManager.cancelUniqueWork(NewsSyncWorker.UNIQUE_ONE_TIME)
        workManager.cancelUniqueWork(ScheduleSyncWorker.UNIQUE_ONE_TIME)
    }

    fun refreshChartNow(context: Context) =
        enqueueOneTime<ChartSyncWorker>(context, ChartSyncWorker.UNIQUE_ONE_TIME)

    fun refreshNewsNow(context: Context) =
        enqueueOneTime<NewsSyncWorker>(context, NewsSyncWorker.UNIQUE_ONE_TIME)

    fun refreshScheduleNow(context: Context) =
        enqueueOneTime<ScheduleSyncWorker>(context, ScheduleSyncWorker.UNIQUE_ONE_TIME)

    private inline fun <reified W : ListenableWorker> enqueueOneTime(context: Context, uniqueName: String) {
        val request = OneTimeWorkRequestBuilder<W>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(
            ChartWidgetReceiver::class.java,
            NewsWidgetReceiver::class.java,
            ScheduleWidgetReceiver::class.java
        ).any { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
