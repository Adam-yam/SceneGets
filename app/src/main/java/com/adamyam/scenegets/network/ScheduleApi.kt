package com.adamyam.scenegets.network

import com.adamyam.scenegets.data.SchedulePeriod
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.models.ScheduleResponse
import kotlinx.serialization.json.Json
import java.time.LocalDate

object ScheduleApi {
    private val json = Json { ignoreUnknownKeys = true }

    /** 특정 반기 파일 하나를 가져온다. 아직 발행되지 않은 다음 반기는 404가 정상 케이스다. */
    suspend fun fetchPeriod(period: SchedulePeriod.Period): ApiResult<ScheduleResponse> {
        val url = SceneFlixConfig.BASE_URL + SceneFlixConfig.schedulePath(period.fileName)
        return when (val outcome = SceneFlixHttpClient.getRaw(url)) {
            is FetchOutcome.Success -> try {
                ApiResult.Success(json.decodeFromString(ScheduleResponse.serializer(), outcome.body))
            } catch (e: Exception) {
                ApiResult.Error("스케줄 파싱 실패 (${period.fileName}): ${e.message}", e)
            }
            is FetchOutcome.Failure -> ApiResult.Error(outcome.message, outcome.cause, outcome.isNotFound)
        }
    }

    /**
     * 오늘 이후의 예정된 일정만 모아서 반환한다.
     * 현재 반기 파일에서 다가오는 일정이 [minUpcoming]개 미만이면, 다음 반기 파일도 시도해서 이어붙인다.
     * 다음 반기 파일이 아직 없으면(404) 에러로 취급하지 않고 조용히 건너뛴다 — 이건 정상 상황이기 때문.
     */
    suspend fun fetchUpcoming(
        today: LocalDate = LocalDate.now(),
        minUpcoming: Int = 3
    ): ApiResult<List<ScheduleEvent>> {
        val currentPeriod = SchedulePeriod.current(today)
        val currentResult = fetchPeriod(currentPeriod)

        val currentEvents = when (currentResult) {
            is ApiResult.Success -> currentResult.data.events
            is ApiResult.Error -> {
                // 현재 반기 파일 자체가 없는 건 비정상이므로 그대로 에러 전파
                return currentResult
            }
        }

        val upcomingFromCurrent = currentEvents.filterUpcoming(today)

        val combined = if (upcomingFromCurrent.size < minUpcoming) {
            val nextPeriod = SchedulePeriod.next(currentPeriod)
            when (val nextResult = fetchPeriod(nextPeriod)) {
                is ApiResult.Success ->
                    upcomingFromCurrent + nextResult.data.events.filterUpcoming(today)
                is ApiResult.Error ->
                    // 다음 반기 파일 미발행(404) 등은 정상 상황 -> 현재 데이터만으로 진행
                    upcomingFromCurrent
            }
        } else {
            upcomingFromCurrent
        }

        return ApiResult.Success(combined.sortedWith(compareBy({ it.date }, { it.time })))
    }

    private fun List<ScheduleEvent>.filterUpcoming(today: LocalDate): List<ScheduleEvent> {
        val todayStr = today.toString() // yyyy-MM-dd (LocalDate.toString()과 데이터 포맷 동일)
        return filter { it.date >= todayStr }
    }
}
