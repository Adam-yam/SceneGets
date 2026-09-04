package com.adamyam.scenegets.widget.news

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.widget.common.WidgetImageLoader

class NewsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = NewsRepository(context).cachedOrLoading()
        val images = when (state) {
            is com.adamyam.scenegets.data.WidgetState.Loaded -> state.data.articles.associate {
                it.url to WidgetImageLoader.load(context, it.thumbnail)
            }
            else -> emptyMap()
        }
        provideContent {
            NewsWidgetContent(state, images)
        }
    }
}
