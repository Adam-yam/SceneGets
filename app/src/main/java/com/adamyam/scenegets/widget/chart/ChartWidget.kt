package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.widget.common.WidgetThemedContent

class ChartWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ChartRepository(context).cachedOrLoading()
        val albumImages = if (state is WidgetState.Loaded) {
            ImageCache.loadAll(context, state.data.songs.mapNotNull { it.albumImageUrl })
        } else {
            emptyMap()
        }

        provideContent {
            WidgetThemedContent {
                ChartWidgetContent(state, albumImages)
            }
        }
    }
}
