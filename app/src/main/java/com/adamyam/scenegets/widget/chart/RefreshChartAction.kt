package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.adamyam.scenegets.data.ChartRepository

/** 위젯 헤더의 새로고침 아이콘을 눌렀을 때: 즉시 네트워크 재조회 후 위젯 갱신 */
class RefreshChartAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ChartRepository(context).refresh()
        ChartWidget().update(context, glanceId)
    }
}
