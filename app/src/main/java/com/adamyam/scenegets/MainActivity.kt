package com.adamyam.scenegets

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chartRepository = ChartRepository(applicationContext)
        newsRepository = NewsRepository(applicationContext)
        scheduleRepository = ScheduleRepository(applicationContext)

        val chromeColor = Color.parseColor("#F2F2F7")
        window.statusBarColor = chromeColor
        window.navigationBarColor = chromeColor

        // 엣지투엣지를 명시적으로 켜서, Android 버전/제조사(One UI 등)에 관계없이
        // 시스템 바 인셋을 우리가 직접 계산해 웹뷰 콘텐츠에 반영한다.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        var lastInsetTop = 0
        var lastInsetBottom = 0

        fun applyInsetVars() {
            webView.evaluateJavascript(
                "document.documentElement.style.setProperty('\u002d\u002dsystem-top','${lastInsetTop}px');" +
                    "document.documentElement.style.setProperty('\u002d\u002dsystem-bottom','${lastInsetBottom}px');",
                null
            )
        }

        webView = WebView(this).apply {
            setBackgroundColor(chromeColor)
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
                    // 페이지가 새로 로드된 직후에도 최신 인셋 값을 다시 주입한다.
                    applyInsetVars()
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(SceneGetsBridge(), "SceneGetsBridge")
        }

        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            lastInsetTop = bars.top
            lastInsetBottom = bars.bottom
            applyInsetVars()
            insets
        }

        setContentView(webView)
        ViewCompat.requestApplyInsets(webView)
        webView.loadUrl("file:///android_asset/scenegets_app_design.html")
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
}
