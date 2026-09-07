package com.adamyam.scenegets

import android.app.Application
import com.adamyam.scenegets.work.WidgetWorkScheduler

class SceneGetsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetWorkScheduler.scheduleAll(this)
    }
}
