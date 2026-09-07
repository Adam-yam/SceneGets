package com.adamyam.scenegets

import android.app.Application
import com.adamyam.scenegets.work.WidgetWorkScheduler

class SceneGetsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱이 한 번이라도 실행되면(위젯 추가 시 자동 실행됨) 주기적 자동 갱신 작업을 등록
        WidgetWorkScheduler.scheduleAll(this)
    }
}
