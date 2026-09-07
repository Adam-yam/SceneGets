package com.adamyam.scenegets.widget.news

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.common.WidgetThemedContent

class NewsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = NewsRepository(context).cachedOrLoading()
        val thumbnails = if (state is WidgetState.Loaded) {
            ImageCache.loadAll(context, state.data.articles.mapNotNull { it.thumbnail })
        } else {
            emptyMap()
        }

        provideContent {
            WidgetThemedContent {
                NewsWidgetContent(state, thumbnails)
            }
        }
    }
}
