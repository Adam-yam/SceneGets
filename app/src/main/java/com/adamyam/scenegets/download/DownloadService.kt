package com.adamyam.scenegets.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.ffmpegkit_maintained.ytdlp.compat.YoutubeDL
import dev.ffmpegkit_maintained.ytdlp.compat.YoutubeDLRequest
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {
    private val cancelRequested = AtomicBoolean(false)
    private var currentUrl: String? = null
    private var currentProcessId: String? = null
    private var lastProgress = -1
    private var lastNotificationAt = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("SceneGets", "다운로드 준비 중", 0, false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelRequested.set(true)
                DownloadStore.cancelled(this)
                runCatching { YoutubeDL.getInstance().destroyProcessById(currentProcessId ?: "") }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url.isNullOrBlank() || ShareUrlParser.extract(url, null) == null) {
                    DownloadStore.failed(this, "유효하지 않은 링크입니다.")
                    stopSelfResult(startId)
                    return START_NOT_STICKY
                }
                if (currentUrl != null) return START_NOT_STICKY
                currentUrl = url
                cancelRequested.set(false)
                Thread { download(url, startId) }.start()
            }
        }
        return START_NOT_STICKY
    }

    private fun download(url: String, startId: Int) {
        try {
            DownloadStore.updateProgress(this, 0, "다운로드 준비 중", "yt-dlp를 초기화하고 있습니다.")
            updateNotification("다운로드 준비 중", 0, true)

            YoutubeDL.init(applicationContext)

            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SceneGets"
            )
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw IllegalStateException("Downloads/SceneGets 폴더를 만들 수 없습니다.")
            }

            // User-supplied text is used only as the URL. All yt-dlp options are fixed here.
            // This prevents a shared string from being interpreted as an option/command.
            val template = File(outputDir, "%(title)s [%(id)s].%(ext)s").absolutePath
            val request = YoutubeDLRequest(url)
                .setOutputTemplate(template)
                .addOption("--no-playlist")
                .addOption("--no-overwrites")
                .addOption("--newline")
                // The maintained yt-dlp Android AAR does not bundle FFmpeg. Prefer a
                // single-file progressive format so a normal download never depends on a
                // separate post-processing binary. When no MP4 is available, fall back to
                // yt-dlp's best single-file format (which may be WebM).
                .addOption("-f", "best[ext=mp4]/best")

            var lastLine = ""
            val done = CountDownLatch(1)
            val processId = "scenegets-${System.currentTimeMillis()}"
            currentProcessId = processId
            YoutubeDL.executeAsync(request, { progress, _, line ->
                if (cancelRequested.get()) return@executeAsync
                val p = progress.toInt().coerceIn(0, 100)
                lastLine = line.orEmpty()
                val now = System.currentTimeMillis()
                if (p != lastProgress || now - lastNotificationAt > 750L) {
                    lastProgress = p
                    lastNotificationAt = now
                    val status = when {
                        lastLine.contains("Merging formats", true) || lastLine.contains("Post-process", true) -> "영상 합치는 중"
                        p >= 100 -> "마무리 중"
                        else -> "다운로드 중"
                    }
                    DownloadStore.updateProgress(this, p, status, cleanLine(lastLine))
                    updateNotification(status, p, true)
                }
                if (lastLine.contains("ERROR:", true)) {
                    DownloadStore.failed(this, cleanLine(lastLine))
                    done.countDown()
                } else if (p >= 100) {
                    done.countDown()
                }
            }, processId)

            // executeAsync is callback-driven in the Android wrapper. A 100% callback means the
            // yt-dlp download phase completed; then wait for the final file to become stable.
            // Partial .part files are ignored.
            if (!done.await(6, TimeUnit.HOURS)) {
                DownloadStore.failed(this, "다운로드 시간이 너무 오래 걸려 자동으로 중단되었습니다.")
                runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
            }
            if (cancelRequested.get()) {
                DownloadStore.cancelled(this)
            } else {
                val file = waitForCompletedFile(outputDir)
                val snapshot = DownloadStore.snapshot(this)
                if (snapshot.state == DownloadStore.State.FAILED) {
                    updateNotification("다운로드 실패", 0, false)
                } else if (file != null && file.length() > 0L) {
                    DownloadStore.success(this, file.absolutePath)
                    updateNotification("다운로드 완료", 100, false)
                } else {
                    DownloadStore.failed(this, "다운로드가 완료되었지만 결과 파일을 찾지 못했습니다.")
                    updateNotification("다운로드 실패", 0, false)
                }
            }
        } catch (e: Throwable) {
            if (!cancelRequested.get()) {
                DownloadStore.failed(this, readableError(e))
                updateNotification("다운로드 실패", 0, false)
            }
        } finally {
            currentUrl = null
            currentProcessId = null
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelfResult(startId)
        }
    }

    private fun waitForCompletedFile(outputDir: File): File? {
        var previousPath: String? = null
        var previousLength = -1L
        var stableSince = 0L
        val deadline = System.currentTimeMillis() + 60_000L
        while (System.currentTimeMillis() < deadline && !cancelRequested.get()) {
            val file = outputDir.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") && !it.name.endsWith(".temp") }
                ?.maxByOrNull { it.lastModified() }
            if (file != null && file.length() > 0L) {
                val path = file.absolutePath
                val length = file.length()
                if (path == previousPath && length == previousLength) {
                    if (stableSince == 0L) stableSince = System.currentTimeMillis()
                    if (System.currentTimeMillis() - stableSince >= 1000L) return file
                } else {
                    previousPath = path
                    previousLength = length
                    stableSince = System.currentTimeMillis()
                }
            }
            Thread.sleep(300L)
        }
        return null
    }

    private fun cleanLine(value: String): String = value
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(300)

    private fun readableError(error: Throwable): String {
        val cause = generateSequence(error) { it.cause }.lastOrNull()
        return cleanLine(cause?.message ?: error.message ?: "알 수 없는 오류")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SceneGets 다운로드",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "영상 다운로드 진행 상태" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(title: String, text: String, progress: Int, ongoing: Boolean): Notification {
        val openIntent = Intent(this, ShareDownloadActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL)
        val cancelPending = PendingIntent.getService(
            this,
            101,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                if (ongoing) setProgress(100, progress.coerceIn(0, 100), false)
                else setProgress(0, 0, false)
                if (ongoing) addAction(android.R.drawable.ic_menu_close_clear_cancel, "취소", cancelPending)
            }
            .build()
    }

    private fun updateNotification(text: String, progress: Int, ongoing: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification("SceneGets", text, progress, ongoing))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.adamyam.scenegets.action.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.adamyam.scenegets.action.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "url"
        private const val CHANNEL_ID = "scenegets_downloads"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context, url: String): Boolean {
            val current = DownloadStore.snapshot(context)
            if ((current.state == DownloadStore.State.STARTING || current.state == DownloadStore.State.DOWNLOADING) &&
                current.startedAt > 0L && System.currentTimeMillis() - current.startedAt < 6L * 60L * 60L * 1000L
            ) {
                return false
            }
            DownloadStore.reset(context, url)
            val intent = Intent(context, DownloadService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_URL, url)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            return true
        }

        fun cancel(context: Context) {
            context.startService(Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL))
        }
    }
}
