package com.adamyam.scenegets.widget.chart

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.adamyam.scenegets.work.WidgetWorkScheduler

class ChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 위젯이 홈 화면에 새로 추가/갱신될 때 최신 데이터를 한 번 받아오도록 트리거
        WidgetWorkScheduler.refreshChartNow(context)
    }
}
