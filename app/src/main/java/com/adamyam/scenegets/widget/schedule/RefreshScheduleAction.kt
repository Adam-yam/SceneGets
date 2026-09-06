package com.adamyam.scenegets.widget.schedule

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.adamyam.scenegets.data.ScheduleRepository

class RefreshScheduleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ScheduleRepository(context).refresh()
        ScheduleWidget().update(context, glanceId)
    }
}
