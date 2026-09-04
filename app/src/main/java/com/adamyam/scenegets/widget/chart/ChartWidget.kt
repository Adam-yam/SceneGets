package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ChartSong
import com.adamyam.scenegets.widget.common.WidgetImageLoader

class ChartWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ChartRepository(context).cachedOrLoading()
        val images = when (state) {
            is WidgetState.Loaded -> state.data.songs.associate {
                songKey(it) to WidgetImageLoader.load(context, it.albumImageUrl)
            }
            else -> emptyMap()
        }
        provideContent {
            ChartWidgetContent(state, images)
        }
    }

    private fun songKey(song: ChartSong): String = "${song.songName}\u0000${song.artistName}"
}
