package com.adamyam.scenegets

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.MemberBirthdays
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.ScheduleRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.notify.ScheduleNotificationManager
import java.io.ByteArrayInputStream
import kotlin.math.ceil
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.models.ScheduleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var rootContainer: FrameLayout
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { encodeDefaults = true }
    private val refreshRunning = AtomicBoolean(false)

    private lateinit var chartRepository: ChartRepository
    private lateinit var newsRepository: NewsRepository
    private lateinit var scheduleRepository: ScheduleRepository

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private var themeMode: String = THEME_SYSTEM
    private var pendingOpenTab: String? = null

    private val thumbnailTargetPx: Int by lazy {
        ceil(THUMBNAIL_MAX_DP * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chartRepository = ChartRepository(applicationContext)
        newsRepository = NewsRepository(applicationContext)
        scheduleRepository = ScheduleRepository(applicationContext)

        themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        pendingOpenTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        ScheduleNotificationManager.ensureChannel(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            setOnLongClickListener { true }
            isHapticFeedbackEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    applyInsetVars()
                }
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return false
                    return !(url.scheme == "file" && url.path?.startsWith("/android_asset/") == true)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url ?: return null
                    if (request.method != "GET") return null
                    val scheme = url.scheme
                    if (scheme != "http" && scheme != "https") return null
                    val path = url.path?.lowercase() ?: return null
                    if (IMAGE_EXTENSIONS.none { path.endsWith(it) }) return null
                    val bytes = ImageCache.loadEncoded(
                        applicationContext,
                        url.toString(),
                        thumbnailTargetPx
                    ) ?: return null
                    return WebResourceResponse("image/jpeg", "UTF-8", ByteArrayInputStream(bytes))
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(SceneGetsBridge(), "SceneGetsBridge")
        }
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(if (isDarkTheme()) Color.BLACK else Color.parseColor("#F2F2F7"))
            addView(
                webView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val lp = webView.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = bars.left
            lp.topMargin = bars.top
            lp.rightMargin = bars.right
            lp.bottomMargin = bars.bottom
            webView.layoutParams = lp
            applyInsetVars()
            insets
        }

        applySystemBars()
        setContentView(rootContainer)
        ViewCompat.requestApplyInsets(rootContainer)
        webView.loadUrl("file:///android_asset/scenegets_app_design.html")
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                "window.SceneGetsWeb && window.SceneGetsWeb.onResume && window.SceneGetsWeb.onResume();",
                null
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenTab = intent.getStringExtra(EXTRA_OPEN_TAB)
        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                "window.SceneGetsWeb && window.SceneGetsWeb.openTabFromNotification && " +
                    "window.SceneGetsWeb.openTabFromNotification();",
                null
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION && ::webView.isInitialized) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            webView.evaluateJavascript(
                "window.SceneGetsWeb && window.SceneGetsWeb.onNotificationPermissionResult && " +
                    "window.SceneGetsWeb.onNotificationPermissionResult($granted);",
                null
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (themeMode == THEME_SYSTEM) {
            applySystemBars()
            if (::webView.isInitialized) {
                webView.evaluateJavascript(
                    "window.SceneGetsWeb && window.SceneGetsWeb.applyTheme && window.SceneGetsWeb.applyTheme('system');",
                    null
                )
            }
        }
    }

    private fun isDarkTheme(): Boolean = when (themeMode) {
        THEME_DARK -> true
        THEME_LIGHT -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun applySystemBars() {
        val dark = isDarkTheme()
        val chromeColor = if (dark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        window.statusBarColor = chromeColor
        window.navigationBarColor = chromeColor
        if (::webView.isInitialized) webView.setBackgroundColor(chromeColor)
        if (::rootContainer.isInitialized) rootContainer.setBackgroundColor(chromeColor)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    private fun applyInsetVars() {
        webView.evaluateJavascript(
            "document.documentElement.style.setProperty('\u002d\u002dsystem-top','0px');" +
                "document.documentElement.style.setProperty('\u002d\u002dsystem-bottom','0px');",
            null
        )
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openBatteryOptimizationSettings() {
        val candidates = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            candidates += Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        }
        candidates += Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        candidates += Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        for (intent in candidates) {
            val ok = runCatching { startActivity(intent) }.isSuccess
            if (ok) return
        }
    }

    private fun sendData(
        chart: ChartResponse,
        news: NewsResponse,
        schedule: List<ScheduleEvent>,
        chartFetchedAt: Long,
        newsFetchedAt: Long,
        scheduleFetchedAt: Long,
        hasError: Boolean
    ) {
        val actual = "{" +
            "\"chart\":" + json.encodeToString(chart) + "," +
            "\"news\":" + json.encodeToString(news) + "," +
            "\"schedule\":{" + "\"events\":" + json.encodeToString(schedule) + "}," +
            "\"chartFetchedAt\":" + chartFetchedAt + "," +
            "\"newsFetchedAt\":" + newsFetchedAt + "," +
            "\"scheduleFetchedAt\":" + scheduleFetchedAt + "," +
            "\"hasError\":" + hasError +
            "}"
        val quoted = JSONObject.quote(actual)
        webView.post {
            if (!isFinishing && !isDestroyed) {
                webView.evaluateJavascript("window.SceneGetsWeb && window.SceneGetsWeb.onData($quoted);", null)
            }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        val chartJob = async { chartRepository.refresh() }
        val newsJob = async { newsRepository.refresh() }
        val scheduleJob = async { scheduleRepository.refresh() }
        val chartFresh = chartJob.await()
        val newsFresh = newsJob.await()
        val scheduleFresh = scheduleJob.await()

        val chart = (chartFresh as? WidgetState.Loaded)?.data ?: ChartResponse()
        val news = (newsFresh as? WidgetState.Loaded)?.data ?: NewsResponse()
        val schedule = MemberBirthdays.mergeInto((scheduleFresh as? WidgetState.Loaded)?.data ?: emptyList())

        val hasError = listOf(chartFresh, newsFresh, scheduleFresh).any {
            it is WidgetState.Failed || (it is WidgetState.Loaded && it.isStale)
        }

        ScheduleNotificationManager.rescheduleAll(applicationContext, schedule)

        sendData(
            chart, news, schedule,
            chartFetchedAt = (chartFresh as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            newsFetchedAt = (newsFresh as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            scheduleFetchedAt = (scheduleFresh as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            hasError = hasError
        )
    }

    private suspend fun loadCacheOnly() {
        val chartCache = chartRepository.cachedOrLoading()
        val newsCache = newsRepository.cachedOrLoading()
        val scheduleCache = scheduleRepository.cachedOrLoading()

        val chart = (chartCache as? WidgetState.Loaded)?.data ?: ChartResponse()
        val news = (newsCache as? WidgetState.Loaded)?.data ?: NewsResponse()
        val schedule = MemberBirthdays.mergeInto((scheduleCache as? WidgetState.Loaded)?.data ?: emptyList())

        val hasError = listOf(chartCache, newsCache, scheduleCache).any {
            it is WidgetState.Loaded && it.isStale
        }

        sendData(
            chart, news, schedule,
            chartFetchedAt = (chartCache as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            newsFetchedAt = (newsCache as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            scheduleFetchedAt = (scheduleCache as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            hasError = hasError
        )
    }

    private inner class SceneGetsBridge {
        @JavascriptInterface
        fun refreshAll(): Boolean {
            if (!refreshRunning.compareAndSet(false, true)) return false
            scope.launch {
                try {
                    loadAll()
                } finally {
                    refreshRunning.set(false)
                }
            }
            return true
        }

        @JavascriptInterface
        fun loadCachedData() {
            scope.launch {
                loadCacheOnly()
            }
        }

        @JavascriptInterface
        fun getTheme(): String = themeMode

        @JavascriptInterface
        fun getResolvedTheme(): String = if (isDarkTheme()) "dark" else "light"

        @JavascriptInterface
        fun setTheme(mode: String?) {
            val next = when (mode) {
                THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> mode
                else -> THEME_SYSTEM
            }
            themeMode = next
            prefs.edit().putString(KEY_THEME, next).apply()
            runOnUiThread {
                applySystemBars()
                if (::webView.isInitialized) {
                    webView.evaluateJavascript(
                        "window.SceneGetsWeb && window.SceneGetsWeb.applyTheme && window.SceneGetsWeb.applyTheme('" + next + "');",
                        null
                    )
                }
            }
        }

        @JavascriptInterface
        fun isBatteryUnrestricted(): Boolean = isIgnoringBatteryOptimizations()

        @JavascriptInterface
        fun openBatterySettings() {
            runOnUiThread { openBatteryOptimizationSettings() }
        }

        @JavascriptInterface
        fun openUrl(url: String?) {
            val uri = runCatching { Uri.parse(url ?: "") }.getOrNull() ?: return
            if (uri.scheme != "http" && uri.scheme != "https") return
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        @JavascriptInterface
        fun openEmail(address: String?) {
            val to = address?.takeIf { it.isNotBlank() } ?: return
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            }
            runCatching { startActivity(intent) }
        }

        @JavascriptInterface
        fun consumePendingOpenTab(): String? {
            val tab = pendingOpenTab
            pendingOpenTab = null
            return tab
        }

        @JavascriptInterface
        fun hasNotificationPermission(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runOnUiThread {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
        }

        @JavascriptInterface
        fun isEventNotificationEnabled(date: String?, time: String?, title: String?): Boolean {
            if (date == null || time == null || title == null) return false
            return ScheduleNotificationManager.isEnabled(applicationContext, date, time, title)
        }

        @JavascriptInterface
        fun toggleEventNotification(date: String?, time: String?, title: String?, enable: Boolean): Boolean {
            if (date == null || time == null || title == null) return false
            ScheduleNotificationManager.ensureChannel(applicationContext)
            return ScheduleNotificationManager.setEventEnabled(applicationContext, date, time, title, enable)
        }

        @JavascriptInterface
        fun sendTestNotification() {
            ScheduleNotificationManager.ensureChannel(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                runOnUiThread {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
                return
            }
            val notification = ScheduleNotificationManager.buildNotification(
                applicationContext,
                "테스트 알림",
                1
            )
            runCatching {
                NotificationManagerCompat.from(applicationContext).notify(TEST_NOTIFICATION_ID, notification)
            }
        }

        @JavascriptInterface
        fun getNotificationSettings(): String {
            val mode = ScheduleNotificationManager.getMode(applicationContext)
            val hours = ScheduleNotificationManager.getLeadHours(applicationContext)
            return JSONObject().put("mode", mode).put("leadHours", hours).toString()
        }

        @JavascriptInterface
        fun setNotificationSettings(mode: String?, leadHours: Int) {
            ScheduleNotificationManager.setSettings(
                applicationContext,
                mode ?: ScheduleNotificationManager.MODE_ONCE,
                leadHours
            )
            scope.launch {
                val cached = scheduleRepository.cachedOrLoading()
                val events = MemberBirthdays.mergeInto((cached as? WidgetState.Loaded)?.data ?: emptyList())
                ScheduleNotificationManager.rescheduleAll(applicationContext, events)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeJavascriptInterface("SceneGetsBridge")
            webView.removeAllViews()
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        private const val PREFS_NAME = "scenegets_settings"
        private const val KEY_THEME = "theme_mode"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"
        private const val THUMBNAIL_MAX_DP = 56
        private const val REQUEST_NOTIFICATION_PERMISSION = 4201
        private const val TEST_NOTIFICATION_ID = 990001
        private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    }
}
