package com.adamyam.scenegets.data

import android.content.Context
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.network.ApiResult
import com.adamyam.scenegets.network.ChartApi
import kotlinx.coroutines.sync.withLock

class ChartRepository(context: Context) {
    private val cache = WidgetCache(context.applicationContext)

    suspend fun refresh(): WidgetState<ChartResponse> = RefreshLocks.chart.withLock {
        val previous = cache.loadChart()?.data
        val etag = cache.getEtag(ETAG_KEY)
        return when (val result = ChartApi.fetchChart(etag, previous)) {
            is ApiResult.Success -> {
                cache.saveChart(result.data)
                cache.saveEtag(ETAG_KEY, result.etag)
                WidgetState.Loaded(
                    result.data,
                    result.fetchedAt,
                    isStale = false,
                    contentUnchanged = previous == result.data
                )
            }
            is ApiResult.Error -> {
                cache.saveChartError(result.message)
                val cached = cache.loadChart()
                if (cached != null) {
                    WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = true, errorMessage = result.message)
                } else {
                    WidgetState.Failed(result.message)
                }
            }
        }
    }

    suspend fun cachedOrLoading(): WidgetState<ChartResponse> {
        val cached = cache.loadChart()
        return if (cached != null) {
            WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = cached.lastError != null, errorMessage = cached.lastError)
        } else {
            WidgetState.Loading
        }
    }

    companion object {
        private const val ETAG_KEY = "chart"
    }
}
