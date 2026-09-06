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

/**
 * 마지막으로 성공한 데이터 + 시각 + (있다면) 마지막 실패 메시지를 저장.
 * 위젯이 켜지자마자, 그리고 네트워크 실패 시 이 캐시로 폴백한다.
 */
class WidgetCache(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // ---------- 차트 ----------
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

    // ---------- 뉴스 ----------
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

    // ---------- 스케줄 (다가오는 일정 리스트를 그대로 캐시) ----------
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

    // ---------- ETag (엔드포인트별 조건부 GET용) ----------
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

    // ---------- 스케줄 반기 파일 원본 캐시 (304 응답 시 재파싱 없이 재구성하기 위함) ----------
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
