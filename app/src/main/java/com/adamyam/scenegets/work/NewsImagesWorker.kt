package com.adamyam.scenegets.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.widget.common.WidgetImageLoader
import com.adamyam.scenegets.widget.news.NewsWidget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Downloads news thumbnails outside provideGlance so a slow image server
 * can never prevent the widget from rendering. */
class NewsImagesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val state = NewsRepository(applicationContext).cachedOrLoading()
        if (state is com.adamyam.scenegets.data.WidgetState.Loaded) {
            coroutineScope {
                state.data.articles.mapNotNull { article ->
                    val url = article.thumbnail?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    async { WidgetImageLoader.load(applicationContext, url) }
                }.awaitAll()
            }
            NewsWidget().updateAll(applicationContext)
        }
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
