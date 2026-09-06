package com.adamyam.scenegets.network

import com.adamyam.scenegets.models.ChartResponse
import kotlinx.serialization.json.Json

object ChartApi {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchChart(etag: String? = null, cachedData: ChartResponse? = null): ApiResult<ChartResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.CHART_PATH
        return when (val outcome = SceneFlixHttpClient.getRaw(url, etag)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(ChartResponse.serializer(), outcome.body), etag = outcome.etag)
            } catch (e: Exception) {
                ApiResult.Error("차트 데이터 파싱 실패: ${e.message}", e)
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
