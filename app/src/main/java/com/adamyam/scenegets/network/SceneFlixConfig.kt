package com.adamyam.scenegets.network

object SceneFlixConfig {
    const val BASE_URL = "https://adam-yam.github.io/SCENE-FLIX/data/"

    const val CHART_PATH = "charts/chart.json"
    const val NEWS_PATH = "news.json"

    fun schedulePath(fileName: String) = "schedule/$fileName"
}
