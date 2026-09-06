package com.adamyam.scenegets.download

import android.content.Context
/** Small persistent state store used by the share/download UI and foreground service. */
object DownloadStore {
    private const val PREFS = "scenegets_downloads"
    private const val KEY_STATE = "state"
    private const val KEY_URL = "url"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_STATUS = "status"
    private const val KEY_MESSAGE = "message"
    private const val KEY_STARTED = "started"
    private const val KEY_FINISHED = "finished"
    private const val KEY_FILE = "file"

    enum class State { IDLE, STARTING, DOWNLOADING, SUCCESS, FAILED, CANCELLED }

    data class Snapshot(
        val state: State,
        val url: String,
        val progress: Int,
        val status: String,
        val message: String,
        val startedAt: Long,
        val finishedAt: Long,
        val filePath: String?
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun reset(context: Context, url: String) {
        prefs(context).edit()
            .clear()
            .putString(KEY_STATE, State.STARTING.name)
            .putString(KEY_URL, url)
            .putInt(KEY_PROGRESS, 0)
            .putString(KEY_STATUS, "다운로드 준비 중")
            .putString(KEY_MESSAGE, "링크를 분석하고 있습니다.")
            .putLong(KEY_STARTED, System.currentTimeMillis())
            .apply()
    }

    fun updateProgress(context: Context, progress: Int, status: String, message: String = "") {
        prefs(context).edit()
            .putString(KEY_STATE, State.DOWNLOADING.name)
            .putInt(KEY_PROGRESS, progress.coerceIn(0, 100))
            .putString(KEY_STATUS, status)
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun success(context: Context, filePath: String?) {
        prefs(context).edit()
            .putString(KEY_STATE, State.SUCCESS.name)
            .putInt(KEY_PROGRESS, 100)
            .putString(KEY_STATUS, "다운로드 완료")
            .putString(KEY_MESSAGE, "파일이 Downloads/SceneGets에 저장되었습니다.")
            .putString(KEY_FILE, filePath)
            .putLong(KEY_FINISHED, System.currentTimeMillis())
            .apply()
    }

    fun failed(context: Context, message: String) {
        prefs(context).edit()
            .putString(KEY_STATE, State.FAILED.name)
            .putString(KEY_STATUS, "다운로드 실패")
            .putString(KEY_MESSAGE, message.take(500))
            .putLong(KEY_FINISHED, System.currentTimeMillis())
            .apply()
    }

    fun cancelled(context: Context) {
        prefs(context).edit()
            .putString(KEY_STATE, State.CANCELLED.name)
            .putString(KEY_STATUS, "다운로드 취소됨")
            .putString(KEY_MESSAGE, "다운로드가 취소되었습니다.")
            .putLong(KEY_FINISHED, System.currentTimeMillis())
            .apply()
    }

    fun snapshot(context: Context): Snapshot {
        val p = prefs(context)
        return Snapshot(
            state = runCatching { State.valueOf(p.getString(KEY_STATE, State.IDLE.name)!!) }.getOrDefault(State.IDLE),
            url = p.getString(KEY_URL, "") ?: "",
            progress = p.getInt(KEY_PROGRESS, 0),
            status = p.getString(KEY_STATUS, "") ?: "",
            message = p.getString(KEY_MESSAGE, "") ?: "",
            startedAt = p.getLong(KEY_STARTED, 0L),
            finishedAt = p.getLong(KEY_FINISHED, 0L),
            filePath = p.getString(KEY_FILE, null)
        )
    }
}

object ShareUrlParser {
    private val urlRegex = Regex("https?://[^\\s<>\"]+", RegexOption.IGNORE_CASE)

    fun extract(rawText: String?, data: android.net.Uri?): String? {
        val candidates = buildList {
            data?.toString()?.let(::add)
            rawText?.let { text -> urlRegex.find(text)?.value?.let(::add) }
        }
        return candidates.asSequence()
            .map { it.trim().trimEnd('.', ',', ';', ')', ']', '}') }
            .mapNotNull { normalize(it) }
            .firstOrNull()
    }

    private fun normalize(value: String): String? {
        val uri = runCatching { android.net.Uri.parse(value) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https")) return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null) return null
        return value
    }
}
