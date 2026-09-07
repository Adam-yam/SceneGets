package com.adamyam.scenegets.data

import android.content.Context
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.network.ApiResult
import com.adamyam.scenegets.network.NewsApi
import kotlinx.coroutines.sync.withLock

class NewsRepository(context: Context) {
    private val cache = WidgetCache(context.applicationContext)

    suspend fun refresh(): WidgetState<NewsResponse> = RefreshLocks.news.withLock {
        val previous = cache.loadNews()?.data
        val etag = cache.getEtag(ETAG_KEY)
        return when (val result = NewsApi.fetchNews(etag, previous)) {
            is ApiResult.Success -> {
                cache.saveNews(result.data)
                cache.saveEtag(ETAG_KEY, result.etag)
                WidgetState.Loaded(
                    result.data,
                    result.fetchedAt,
                    isStale = false,
                    contentUnchanged = previous == result.data
                )
            }
            is ApiResult.Error -> {
                cache.saveNewsError(result.message)
                val cached = cache.loadNews()
                if (cached != null) {
                    WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = true, errorMessage = result.message)
                } else {
                    WidgetState.Failed(result.message)
                }
            }
        }
    }

    suspend fun cachedOrLoading(): WidgetState<NewsResponse> {
        val cached = cache.loadNews()
        return if (cached != null) {
            WidgetState.Loaded(cached.data, cached.fetchedAt, isStale = cached.lastError != null, errorMessage = cached.lastError)
        } else {
            WidgetState.Loading
        }
    }

    companion object {
        private const val ETAG_KEY = "news"
    }
}
