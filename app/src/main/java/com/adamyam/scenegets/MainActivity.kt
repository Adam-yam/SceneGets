package com.adamyam.scenegets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.widget.FrameLayout
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.ScheduleRepository
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.models.ScheduleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * SceneGets 메인 화면.
 *
 * HTML은 assets의 인앱 디자인을 그대로 사용하고,
 * 실제 차트/뉴스/스케줄 데이터는 기존 SCENE-FLIX API + 캐시를 통해 공급한다.
 */
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { encodeDefaults = true }

    private lateinit var chartRepository: ChartRepository
    private lateinit var newsRepository: NewsRepository
    private lateinit var scheduleRepository: ScheduleRepository

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /** "light" | "dark" | "system" */
    private var themeMode: String = THEME_SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chartRepository = ChartRepository(applicationContext)
        newsRepository = NewsRepository(applicationContext)
        scheduleRepository = ScheduleRepository(applicationContext)

        themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

        // Android 15(targetSdk 35)에서는 edge-to-edge가 강제될 수 있으므로,
        // WebView 자체를 시스템 바 영역만큼 안쪽으로 배치해 일반 앱처럼
        // 상태바/내비게이션바와 콘텐츠가 겹치지 않도록 한다.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    applyInsetVars()
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(SceneGetsBridge(), "SceneGetsBridge")
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F2F2F7"))
            addView(webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val lp = webView.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = 0
            lp.topMargin = bars.top
            lp.rightMargin = 0
            lp.bottomMargin = bars.bottom
            webView.layoutParams = lp

            // WebView가 이미 안전 영역 안에 배치되므로 HTML에는 추가 inset을 주지 않는다.
            applyInsetVars()
            insets
        }

        applySystemBars()
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (themeMode == THEME_SYSTEM) applySystemBars()
    }

    private fun isDarkTheme(): Boolean = when (themeMode) {
        THEME_DARK -> true
        THEME_LIGHT -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** 상태바/내비게이션바 색과 아이콘 명암을 현재 테마에 맞춘다. */
    private fun applySystemBars() {
        val dark = isDarkTheme()
        val chromeColor = if (dark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        window.statusBarColor = chromeColor
        window.navigationBarColor = chromeColor
        if (::webView.isInitialized) webView.setBackgroundColor(chromeColor)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    private fun applyInsetVars() {
        // WebView 자체가 시스템 바 영역만큼 안쪽에 배치되므로
        // HTML에는 추가 시스템 여백을 넣지 않는다.
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

    /**
     * 배터리 최적화 예외 요청 화면으로 이동한다.
     * 실패 시 앱 세부 설정 화면으로, 그것도 불가하면 전체 배터리 최적화 목록으로 폴백한다.
     */
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

    private fun sendData(chart: ChartResponse, news: NewsResponse, schedule: List<ScheduleEvent>) {
        // Build one JSON object and quote it as a JavaScript string argument.
        val actual = "{" +
            "\"chart\":" + json.encodeToString(chart) + "," +
            "\"news\":" + json.encodeToString(news) + "," +
            "\"schedule\":{" + "\"updated\":\"\"," + "\"events\":" + json.encodeToString(schedule) + "}" +
            "}"
        val quoted = JSONObject.quote(actual)
        webView.post {
            if (!isFinishing && !isDestroyed) {
                webView.evaluateJavascript("window.SceneGetsWeb && window.SceneGetsWeb.onData($quoted);", null)
            }
        }
    }

    private suspend fun loadAll(sendCacheFirst: Boolean) {
        val chartCache = chartRepository.cachedOrLoading()
        val newsCache = newsRepository.cachedOrLoading()
        val scheduleCache = scheduleRepository.cachedOrLoading()
        if (sendCacheFirst) {
            val chart = (chartCache as? WidgetState.Loaded)?.data ?: ChartResponse()
            val news = (newsCache as? WidgetState.Loaded)?.data ?: NewsResponse()
            val schedule = (scheduleCache as? WidgetState.Loaded)?.data ?: emptyList()
            sendData(chart, news, schedule)
        }

        val chartFresh = chartRepository.refresh()
        val newsFresh = newsRepository.refresh()
        val scheduleFresh = scheduleRepository.refresh()
        val chart = (chartFresh as? WidgetState.Loaded)?.data ?: (chartCache as? WidgetState.Loaded)?.data ?: ChartResponse()
        val news = (newsFresh as? WidgetState.Loaded)?.data ?: (newsCache as? WidgetState.Loaded)?.data ?: NewsResponse()
        val schedule = (scheduleFresh as? WidgetState.Loaded)?.data ?: (scheduleCache as? WidgetState.Loaded)?.data ?: emptyList()
        sendData(chart, news, schedule)
    }

    private inner class SceneGetsBridge {
        @JavascriptInterface
        fun refreshAll() {
            scope.launch {
                loadAll(sendCacheFirst = false)
            }
        }

        @JavascriptInterface
        fun getTheme(): String = themeMode

        @JavascriptInterface
        fun setTheme(mode: String?) {
            val next = when (mode) {
                THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> mode
                else -> THEME_SYSTEM
            }
            themeMode = next
            prefs.edit().putString(KEY_THEME, next).apply()
            runOnUiThread { applySystemBars() }
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

    private companion object {
        const val PREFS_NAME = "scenegets_settings"
        const val KEY_THEME = "theme_mode"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
