package com.adamyam.scenegets.widget.news

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.adamyam.scenegets.data.NewsRepository

class RefreshNewsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NewsRepository(context).refresh()
        NewsWidget().update(context, glanceId)
    }
}
