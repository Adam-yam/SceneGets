package com.adamyam.scenegets.network

import com.adamyam.scenegets.models.NewsResponse
import kotlinx.serialization.json.Json

object NewsApi {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchNews(etag: String? = null, cachedData: NewsResponse? = null): ApiResult<NewsResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.NEWS_PATH
        return when (val outcome = SceneFlixHttpClient.getRaw(url, etag)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(NewsResponse.serializer(), outcome.body), etag = outcome.etag)
            } catch (e: Exception) {
                ApiResult.Error("뉴스 데이터 파싱 실패: ${e.message}", e)
            }
            FetchOutcome.NotModified -> if (cachedData != null) {
                ApiResult.Success(cachedData, etag = etag)
            } else {
                ApiResult.Error("304 응답을 받았지만 캐시된 데이터가 없음")
            }
            is FetchOutcome.Failure -> ApiResult.Error(outcome.message, outcome.cause, outcome.isNotFound)
        }
    }
}
