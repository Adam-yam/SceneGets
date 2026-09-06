package com.adamyam.scenegets.network

/**
 * SCENE-FLIX GitHub Pages가 공개 데이터 백엔드 역할을 함.
 * 필요 시 이 파일의 BASE_URL 하나만 바꾸면 전체 앱의 fetch 대상이 바뀜.
 */
object SceneFlixConfig {
    const val BASE_URL = "https://adam-yam.github.io/SCENE-FLIX/data/"

    const val CHART_PATH = "charts/chart.json"
    const val NEWS_PATH = "news.json"

    fun schedulePath(fileName: String) = "schedule/$fileName"
}
