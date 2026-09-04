package com.adamyam.scenegets.widget.news

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.common.WidgetImageLoader
import com.adamyam.scenegets.work.NewsImagesWorker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class NewsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = NewsRepository(context).cachedOrLoading()

        // Never make the widget's first render wait on the network. A slow or
        // blocked thumbnail server must not turn the whole widget into
        // "Content can't be displayed".
        val images = if (state is WidgetState.Loaded) {
            loadCachedImagesOnly(context, state.data.articles.map { it.url to it.thumbnail })
        } else {
            emptyMap()
        }

        // If images are missing, let WorkManager fetch them in the background
        // and refresh the widget afterwards.
        if (state is WidgetState.Loaded && state.data.articles.any { it.thumbnail?.isNotBlank() == true && !images.containsKey(it.url) }) {
            runCatching {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<NewsImagesWorker>().build()
                )
            }
        }

        provideContent {
            NewsWidgetContent(state, images)
        }
    }

    private suspend fun loadCachedImagesOnly(
        context: Context,
        entries: List<Pair<String, String?>>
    ): Map<String, android.graphics.Bitmap> = coroutineScope {
        // WidgetImageLoader.load() checks disk cache first, but must not be
        // allowed to perform a network request during provideGlance().
        // A short timeout keeps even damaged caches from blocking rendering.
        withTimeoutOrNull(1200L) {
            entries.map { (articleUrl, imageUrl) ->
                async {
                    imageUrl?.let { WidgetImageLoader.loadCached(context, it) }
                        ?.let { articleUrl to it }
                }
            }.awaitAll().filterNotNull().toMap()
        } ?: emptyMap()
    }
}
