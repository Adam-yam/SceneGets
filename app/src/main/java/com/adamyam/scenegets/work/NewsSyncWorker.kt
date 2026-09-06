package com.adamyam.scenegets.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.news.NewsWidget

class NewsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = NewsRepository(applicationContext).refresh()
        if ((state as? WidgetState.Loaded)?.contentUnchanged != true) {
            NewsWidget().updateAll(applicationContext)
        }
        return if (state is WidgetState.Failed) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC = "news_sync_periodic"
        const val UNIQUE_ONE_TIME = "news_sync_manual"
    }
}
