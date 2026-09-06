package com.adamyam.scenegets.download

import android.Manifest
import com.adamyam.scenegets.R
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File

/** Receives Android Share/View intents and presents a small, native download status screen. */
class ShareDownloadActivity : Activity() {
    private lateinit var urlView: TextView
    private lateinit var statusView: TextView
    private lateinit var messageView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var actionButton: Button
    private lateinit var closeButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var pendingUrl: String? = null
    private var notificationPermissionHandled = false

    private val poll = object : Runnable {
        override fun run() {
            render(DownloadStore.snapshot(this@ShareDownloadActivity))
            handler.postDelayed(this, 400L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_download)

        urlView = findViewById(R.id.share_url)
        statusView = findViewById(R.id.share_status)
        messageView = findViewById(R.id.share_message)
        progressView = findViewById(R.id.share_progress)
        actionButton = findViewById(R.id.share_action)
        closeButton = findViewById(R.id.share_close)

        actionButton.setOnClickListener {
            when (DownloadStore.snapshot(this).state) {
                DownloadStore.State.SUCCESS -> openFile(DownloadStore.snapshot(this).filePath)
                DownloadStore.State.FAILED, DownloadStore.State.CANCELLED -> pendingUrl?.let { startDownload(it) }
                DownloadStore.State.DOWNLOADING, DownloadStore.State.STARTING -> DownloadService.cancel(this)
                else -> pendingUrl?.let { startDownload(it) }
            }
        }
        closeButton.setOnClickListener { finish() }

        val url = ShareUrlParser.extract(
            intent.getStringExtra(Intent.EXTRA_TEXT),
            intent.data
        )
        if (url == null) {
            Toast.makeText(this, "공유된 링크를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        pendingUrl = url
        urlView.text = url
        requestPermissionsThenStart(url)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestPermissionsThenStart(url: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionHandled &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
            return
        }

        startDownload(url)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_STORAGE && requestCode != REQUEST_NOTIFICATIONS) return
        val url = pendingUrl ?: return
        // Notification permission is optional for the actual download. Storage permission on
        // Android 9 and below is required because the app writes to public Downloads.
        if (requestCode == REQUEST_NOTIFICATIONS) notificationPermissionHandled = true
        if (requestCode == REQUEST_STORAGE && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        ) {
            statusView.text = "저장 권한이 필요합니다"
            messageView.text = "Android 9 이하에서는 Downloads 폴더에 저장하려면 저장 권한이 필요합니다."
            actionButton.text = "권한 설정"
            actionButton.setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            return
        }
        requestPermissionsThenStart(url)
    }

    private fun startDownload(url: String) {
        pendingUrl = url
        if (!DownloadService.start(this, url)) {
            statusView.text = "이미 다운로드 중입니다"
            messageView.text = "현재 다운로드가 끝난 뒤 다시 시도해 주세요."
            return
        }
        actionButton.text = "취소"
        actionButton.visibility = View.VISIBLE
        statusView.text = "다운로드 준비 중"
    }

    private fun render(snapshot: DownloadStore.Snapshot) {
        progressView.progress = snapshot.progress
        statusView.text = snapshot.status.ifBlank { "다운로드 준비 중" }
        messageView.text = snapshot.message
        actionButton.text = when (snapshot.state) {
            DownloadStore.State.SUCCESS -> "파일 열기"
            DownloadStore.State.FAILED, DownloadStore.State.CANCELLED -> "다시 시도"
            DownloadStore.State.DOWNLOADING, DownloadStore.State.STARTING -> "취소"
            DownloadStore.State.IDLE -> "다운로드"
        }
        if (snapshot.state == DownloadStore.State.SUCCESS) {
            messageView.setTextColor(Color.rgb(45, 125, 70))
        } else if (snapshot.state == DownloadStore.State.FAILED) {
            messageView.setTextColor(Color.rgb(190, 55, 55))
        } else {
            messageView.setTextColor(Color.DKGRAY)
        }
    }

    private fun openFile(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = runCatching { FileProvider.getUriForFile(this, "${packageName}.fileprovider", file) }.getOrNull()
            ?: run { Toast.makeText(this, "파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show(); return }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "동영상을 열 앱이 없습니다. 파일은 Downloads/SceneGets에 저장되어 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    companion object {
        private const val REQUEST_STORAGE = 7101
        private const val REQUEST_NOTIFICATIONS = 7102
    }
}
