package com.adamyam.scenegets.network

import com.adamyam.scenegets.data.SchedulePeriod
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.models.ScheduleResponse
import kotlinx.serialization.json.Json
import java.time.LocalDate

object ScheduleApi {
    private val json = Json { ignoreUnknownKeys = true }

    data class PeriodContext(val etag: String? = null, val cached: ScheduleResponse? = null)
    data class PeriodFetchInfo(val fileName: String, val etag: String?, val data: ScheduleResponse)
    data class UpcomingResult(val events: List<ScheduleEvent>, val periods: List<PeriodFetchInfo>)

    suspend fun fetchPeriod(
        period: SchedulePeriod.Period,
        etag: String? = null,
        cachedData: ScheduleResponse? = null
    ): ApiResult<ScheduleResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.schedulePath(period.fileName)
        return when (val outcome = SceneFlixHttpClient.getRaw(url, etag)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(ScheduleResponse.serializer(), outcome.body), etag = outcome.etag)
            } catch (e: Exception) {
                ApiResult.Error("스케줄 파싱 실패 (${period.fileName}): ${e.message}", e)
            }
            FetchOutcome.NotModified -> if (cachedData != null) {
                ApiResult.Success(cachedData, etag = etag)
            } else {
                ApiResult.Error("304 응답을 받았지만 캐시된 데이터가 없음 (${period.fileName})")
            }
            is FetchOutcome.Failure -> ApiResult.Error(outcome.message, outcome.cause, outcome.isNotFound)
        }
    }

    suspend fun fetchUpcoming(
        today: LocalDate = LocalDate.now(),
        minUpcoming: Int = 3,
        periodContext: suspend (String) -> PeriodContext = { PeriodContext() }
    ): ApiResult<UpcomingResult> {
        val currentPeriod = SchedulePeriod.current(today)
        val currentCtx = periodContext(currentPeriod.fileName)
        val currentResult = fetchPeriod(currentPeriod, currentCtx.etag, currentCtx.cached)

        val currentData = when (currentResult) {
            is ApiResult.Success -> currentResult.data
            is ApiResult.Error -> return currentResult
        }

        val periods = mutableListOf(PeriodFetchInfo(currentPeriod.fileName, currentResult.etag, currentData))
        val currentEvents = currentData.events.filterUpcoming(today)

        val combined = if (currentEvents.size < minUpcoming) {
            val nextPeriod = SchedulePeriod.next(currentPeriod)
            val nextCtx = periodContext(nextPeriod.fileName)
            when (val nextResult = fetchPeriod(nextPeriod, nextCtx.etag, nextCtx.cached)) {
                is ApiResult.Success -> {
                    periods += PeriodFetchInfo(nextPeriod.fileName, nextResult.etag, nextResult.data)
                    currentEvents + nextResult.data.events.filterUpcoming(today)
                }
                is ApiResult.Error -> currentEvents
            }
        } else {
            currentEvents
        }

        return ApiResult.Success(
            UpcomingResult(combined.sortedWith(compareBy({ it.date }, { it.time })), periods)
        )
    }

    private fun List<ScheduleEvent>.filterUpcoming(today: LocalDate): List<ScheduleEvent> {
        val todayStr = today.toString()
        return filter { it.date >= todayStr }
    }
}
