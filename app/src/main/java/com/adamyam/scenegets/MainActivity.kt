package com.adamyam.scenegets

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * 위젯 전용 앱이라 별도 화면은 최소한으로만 둠.
 * 홈 화면에서 위젯을 길게 눌러 추가하는 방식이 메인 진입점.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "SceneGets\n\n홈 화면을 길게 눌러 위젯을 추가해주세요.\n\n· 차트 위젯\n· 뉴스 위젯\n· 스케줄 위젯\n\n위젯이 자동으로 최신 데이터를 갱신합니다\n(차트/뉴스 1시간, 스케줄 6시간)"
            textSize = 15f
            setPadding(48, 96, 48, 48)
        }
        setContentView(textView)
    }
}
