package com.adamyam.scenegets.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 자동 갱신 주기:
 *  - 차트: 1시간 (사이트 갱신 주기와 동일)
 *  - 뉴스: 1시간
 *  - 스케줄: 6시간
 * 앱이 열려있지 않아도 WorkManager가 백그라운드에서 계속 실행함.
 */
object WidgetWorkScheduler {

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniquePeriodicWork(
            ChartSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ChartSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            NewsSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NewsSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            ScheduleSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScheduleSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .build()
        )
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
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
