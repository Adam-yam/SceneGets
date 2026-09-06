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
 *  - 차트: 1시간마다, 매시 정각(0분)에 맞춰 실행
 *  - 뉴스: 2시간마다, 짝수 시 3분에 맞춰 실행
 *  - 스케줄: 2시간마다, 짝수 시 3분에 맞춰 실행 (뉴스와 동일 시각)
 * 앱이 열려있지 않아도 WorkManager가 백그라운드에서 계속 실행함.
 *
 * WorkManager는 initialDelay로 첫 실행 시각만 맞춰줄 뿐, 이후 주기는
 * "그 시각으로부터 N시간 뒤"로 반복되므로 실제 정렬은 Doze/배터리 최적화
 * 영향이 없다는 전제에서 유효하다. 배터리 제한없음이 켜져 있을수록 이 정렬이
 * 안정적으로 유지된다.
 */
object WidgetWorkScheduler {

    private const val CHART_INTERVAL_HOURS = 1
    private const val CHART_TARGET_MINUTE = 0
    private const val NEWS_SCHEDULE_INTERVAL_HOURS = 2
    private const val NEWS_SCHEDULE_TARGET_MINUTE = 3

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniquePeriodicWork(
            ChartSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ChartSyncWorker>(CHART_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(CHART_INTERVAL_HOURS, CHART_TARGET_MINUTE),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(networkConstraints())
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            NewsSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NewsSyncWorker>(NEWS_SCHEDULE_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(NEWS_SCHEDULE_INTERVAL_HOURS, NEWS_SCHEDULE_TARGET_MINUTE),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(networkConstraints())
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            ScheduleSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ScheduleSyncWorker>(NEWS_SCHEDULE_INTERVAL_HOURS.toLong(), TimeUnit.HOURS)
                .setInitialDelay(
                    RefreshAlignment.millisUntilNext(NEWS_SCHEDULE_INTERVAL_HOURS, NEWS_SCHEDULE_TARGET_MINUTE),
                    TimeUnit.MILLISECONDS
                )
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
