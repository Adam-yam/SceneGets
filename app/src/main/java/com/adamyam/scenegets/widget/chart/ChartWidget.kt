package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.WidgetState

class ChartWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 네트워크 호출 없이 캐시부터 즉시 보여주고, 실제 갱신은 WorkManager/RefreshAction이 담당
        val state = ChartRepository(context).cachedOrLoading()

        // Composable 안에서는 네트워크를 탈 수 없으므로, 앨범 커버는 여기서 미리 받아둔다.
        val albumImages = if (state is WidgetState.Loaded) {
            ImageCache.loadAll(context, state.data.songs.mapNotNull { it.albumImageUrl })
        } else {
            emptyMap()
        }

        provideContent {
            ChartWidgetContent(state, albumImages)
        }
    }
}
