package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.adamyam.scenegets.data.ChartRepository

class RefreshChartAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ChartRepository(context).refresh()
        ChartWidget().update(context, glanceId)
    }
}
