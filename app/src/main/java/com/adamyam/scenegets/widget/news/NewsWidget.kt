package com.adamyam.scenegets.widget.news

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.WidgetState

class NewsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = NewsRepository(context).cachedOrLoading()

        // Composable 안에서는 네트워크를 탈 수 없으므로, 썸네일은 여기서 미리 받아둔다.
        val thumbnails = if (state is WidgetState.Loaded) {
            ImageCache.loadAll(context, state.data.articles.mapNotNull { it.thumbnail })
        } else {
            emptyMap()
        }

        provideContent {
            NewsWidgetContent(state, thumbnails)
        }
    }
}
