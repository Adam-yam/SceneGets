package com.adamyam.scenegets.network

import com.adamyam.scenegets.models.ChartResponse
import kotlinx.serialization.json.Json

object ChartApi {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchChart(): ApiResult<ChartResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.CHART_PATH
        return when (val outcome = SceneFlixHttpClient.getRaw(url)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(ChartResponse.serializer(), outcome.body))
            } catch (e: Exception) {
                ApiResult.Error("차트 데이터 파싱 실패: ${e.message}", e)
            }
            is FetchOutcome.Failure -> ApiResult.Error(outcome.message, outcome.cause, outcome.isNotFound)
        }
    }
}
