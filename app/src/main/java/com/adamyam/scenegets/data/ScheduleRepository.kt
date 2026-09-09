package com.adamyam.scenegets.data

import android.content.Context
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.network.ApiResult
import com.adamyam.scenegets.network.ScheduleApi
import kotlinx.coroutines.sync.withLock

class ScheduleRepository(context: Context) {
    private val cache = WidgetCache(context.applicationContext)

    suspend fun refresh(): WidgetState<List<ScheduleEvent>> = RefreshLocks.schedule.withLock {
        val previous = cache.loadSchedule()?.data
        val result = ScheduleApi.fetchUpcoming(
            periodContext = { fileName ->
                ScheduleApi.PeriodContext(
                    etag = cache.getEtag(fileName),
                    cached = cache.loadSchedulePeriodRaw(fileName)
                )
            }
        )
        return when (result) {
            is ApiResult.Success -> {
                val events = result.data.events
                cache.saveSchedule(events)
                result.data.periods.forEach { info ->
                    cache.saveSchedulePeriodRaw(info.fileName, info.data)
                    cache.saveEtag(info.fileName, info.etag)
                }
                cache.cleanupStaleSchedulePeriods(SchedulePeriod.relevantFileNames())
                WidgetState.Loaded(
                    events,
                    result.fetchedAt,
                    isStale = false,
                    contentUnchanged = previous == events
                )
            }
            is ApiResult.Error -> {
                cache.saveScheduleError(result.message)
                val cached = cache.loadSchedule()
                if (cached != null) {
                    WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = true, errorMessage = result.message)
                } else {
                    WidgetState.Failed(result.message)
                }
            }
        }
    }

    suspend fun cachedOrLoading(): WidgetState<List<ScheduleEvent>> {
        val cached = cache.loadSchedule()
        return if (cached != null) {
            WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = cached.lastError != null, errorMessage = cached.lastError)
        } else {
            WidgetState.Loading
        }
    }
}
