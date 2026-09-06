package com.adamyam.scenegets.network

import com.adamyam.scenegets.models.NewsResponse
import kotlinx.serialization.json.Json

object NewsApi {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchNews(): ApiResult<NewsResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.NEWS_PATH
        return when (val outcome = SceneFlixHttpClient.getRaw(url)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(NewsResponse.serializer(), outcome.body))
            } catch (e: Exception) {
                ApiResult.Error("뉴스 데이터 파싱 실패: ${e.message}", e)
            }
            is FetchOutcome.Failure -> ApiResult.Error(outcome.message, outcome.cause, outcome.isNotFound)
        }
    }
}
