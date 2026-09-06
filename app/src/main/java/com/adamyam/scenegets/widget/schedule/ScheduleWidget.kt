package com.adamyam.scenegets.widget.schedule

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ScheduleRepository

class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ScheduleRepository(context).cachedOrLoading()
        provideContent {
            ScheduleWidgetContent(state)
        }
    }
}
