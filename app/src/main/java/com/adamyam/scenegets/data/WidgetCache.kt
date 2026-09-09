package com.adamyam.scenegets.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.models.ScheduleEvent
import com.adamyam.scenegets.models.ScheduleResponse
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.widgetDataStore by preferencesDataStore(name = "scenegets_widget_cache")

data class CachedData<T>(val data: T, val fetchedAt: Long, val lastError: String? = null)

class WidgetCache(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    suspend fun saveChart(data: ChartResponse) {
        val now = System.currentTimeMillis()
        context.widgetDataStore.edit { prefs ->
            prefs[CHART_JSON] = json.encodeToString(ChartResponse.serializer(), data)
            prefs[CHART_FETCHED_AT] = now
            prefs.remove(CHART_LAST_ERROR)
        }
    }

    suspend fun saveChartError(message: String) {
        context.widgetDataStore.edit { prefs -> prefs[CHART_LAST_ERROR] = message }
    }

    suspend fun loadChart(): CachedData<ChartResponse>? {
        val prefs = context.widgetDataStore.data.first()
        val raw = prefs[CHART_JSON] ?: return null
        val data = runCatching { json.decodeFromString(ChartResponse.serializer(), raw) }.getOrNull() ?: return null
        return CachedData(data, prefs[CHART_FETCHED_AT] ?: 0L, prefs[CHART_LAST_ERROR])
    }
    suspend fun saveNews(data: NewsResponse) {
        val now = System.currentTimeMillis()
        context.widgetDataStore.edit { prefs ->
            prefs[NEWS_JSON] = json.encodeToString(NewsResponse.serializer(), data)
            prefs[NEWS_FETCHED_AT] = now
            prefs.remove(NEWS_LAST_ERROR)
        }
    }

    suspend fun saveNewsError(message: String) {
        context.widgetDataStore.edit { prefs -> prefs[NEWS_LAST_ERROR] = message }
    }

    suspend fun loadNews(): CachedData<NewsResponse>? {
        val prefs = context.widgetDataStore.data.first()
        val raw = prefs[NEWS_JSON] ?: return null
        val data = runCatching { json.decodeFromString(NewsResponse.serializer(), raw) }.getOrNull() ?: return null
        return CachedData(data, prefs[NEWS_FETCHED_AT] ?: 0L, prefs[NEWS_LAST_ERROR])
    }
    private val scheduleEventListSerializer = ListSerializer(ScheduleEvent.serializer())

    suspend fun saveSchedule(events: List<ScheduleEvent>) {
        val now = System.currentTimeMillis()
        context.widgetDataStore.edit { prefs ->
            prefs[SCHEDULE_JSON] = json.encodeToString(scheduleEventListSerializer, events)
            prefs[SCHEDULE_FETCHED_AT] = now
            prefs.remove(SCHEDULE_LAST_ERROR)
        }
    }

    suspend fun saveScheduleError(message: String) {
        context.widgetDataStore.edit { prefs -> prefs[SCHEDULE_LAST_ERROR] = message }
    }

    suspend fun loadSchedule(): CachedData<List<ScheduleEvent>>? {
        val prefs = context.widgetDataStore.data.first()
        val raw = prefs[SCHEDULE_JSON] ?: return null
        val data = runCatching { json.decodeFromString(scheduleEventListSerializer, raw) }.getOrNull() ?: return null
        return CachedData(data, prefs[SCHEDULE_FETCHED_AT] ?: 0L, prefs[SCHEDULE_LAST_ERROR])
    }
    suspend fun getEtag(key: String): String? {
        val prefs = context.widgetDataStore.data.first()
        return prefs[stringPreferencesKey("etag_$key")]
    }

    suspend fun saveEtag(key: String, etag: String?) {
        context.widgetDataStore.edit { prefs ->
            val prefKey = stringPreferencesKey("etag_$key")
            if (etag != null) {
                prefs[prefKey] = etag
            } else {
                prefs.remove(prefKey)
            }
        }
    }
    suspend fun saveSchedulePeriodRaw(fileName: String, data: ScheduleResponse) {
        context.widgetDataStore.edit { prefs ->
            prefs[stringPreferencesKey("schedule_raw_$fileName")] =
                json.encodeToString(ScheduleResponse.serializer(), data)
        }
    }

    suspend fun loadSchedulePeriodRaw(fileName: String): ScheduleResponse? {
        val prefs = context.widgetDataStore.data.first()
        val raw = prefs[stringPreferencesKey("schedule_raw_$fileName")] ?: return null
        return runCatching { json.decodeFromString(ScheduleResponse.serializer(), raw) }.getOrNull()
    }

    suspend fun cleanupStaleSchedulePeriods(keepFileNames: Set<String>) {
        context.widgetDataStore.edit { prefs ->
            val staleKeys = prefs.asMap().keys.filter { key ->
                val name = key.name
                val fileName = when {
                    name.startsWith("schedule_raw_") -> name.removePrefix("schedule_raw_")
                    name.startsWith("etag_schedule_") -> name.removePrefix("etag_")
                    else -> null
                } ?: return@filter false
                fileName !in keepFileNames
            }
            staleKeys.forEach { prefs.remove(it) }
        }
    }

    companion object {
        private val CHART_JSON = stringPreferencesKey("chart_json")
        private val CHART_FETCHED_AT = longPreferencesKey("chart_fetched_at")
        private val CHART_LAST_ERROR = stringPreferencesKey("chart_last_error")

        private val NEWS_JSON = stringPreferencesKey("news_json")
        private val NEWS_FETCHED_AT = longPreferencesKey("news_fetched_at")
        private val NEWS_LAST_ERROR = stringPreferencesKey("news_last_error")

        private val SCHEDULE_JSON = stringPreferencesKey("schedule_json")
        private val SCHEDULE_FETCHED_AT = longPreferencesKey("schedule_fetched_at")
        private val SCHEDULE_LAST_ERROR = stringPreferencesKey("schedule_last_error")
    }
}
