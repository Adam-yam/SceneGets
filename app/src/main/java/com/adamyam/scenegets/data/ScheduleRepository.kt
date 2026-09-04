package com.adamyam.scenegets.data

import android.content.Context
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.network.ApiResult
import com.adamyam.scenegets.network.ScheduleApi

class ScheduleRepository(context: Context) {
    private val cache = WidgetCache(context.applicationContext)

    suspend fun refresh(): WidgetState<List<ScheduleEvent>> {
        return when (val result = ScheduleApi.fetchUpcoming()) {
            is ApiResult.Success -> {
                cache.saveSchedule(result.data)
                WidgetState.Loaded(result.data, result.fetchedAt, isStale = false)
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
