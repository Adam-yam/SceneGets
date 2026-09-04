package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository

class ChartWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 네트워크 호출 없이 캐시부터 즉시 보여주고, 실제 갱신은 WorkManager/RefreshAction이 담당
        val state = ChartRepository(context).cachedOrLoading()
        provideContent {
            ChartWidgetContent(state)
        }
    }
}
