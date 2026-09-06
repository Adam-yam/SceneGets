package com.adamyam.scenegets

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.MemberBirthdays
import com.adamyam.scenegets.data.NewsRepository
import com.adamyam.scenegets.data.ScheduleRepository
import com.adamyam.scenegets.data.WidgetState
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
    private lateinit var rootContainer: FrameLayout
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { encodeDefaults = true }

    private lateinit var chartRepository: ChartRepository
    private lateinit var newsRepository: NewsRepository
    private lateinit var scheduleRepository: ScheduleRepository

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /** "light" | "dark" | "system" */
    private var themeMode: String = THEME_SYSTEM

    private val thumbnailTargetPx: Int by lazy {
        ceil(THUMBNAIL_MAX_DP * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chartRepository = ChartRepository(applicationContext)
        newsRepository = NewsRepository(applicationContext)
        scheduleRepository = ScheduleRepository(applicationContext)

        themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

        // Android 15(targetSdk 35)부터 edge-to-edge가 기본/강제되므로,
        // 시스템이 콘텐츠를 자동으로 밀어준다고 가정하지 않는다.
        // 앱이 직접 WindowInsets를 받아 콘텐츠 영역을 결정한다.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 앱은 file:///android_asset/의 번들 HTML만 로드하며 임의 파일시스템 접근이
            // 필요 없다. addJavascriptInterface와 함께 켜두면 공격 표면만 늘어나므로 끈다.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            // CSS의 user-select:none만으로는 일부 기기에서 길게 눌렀을 때
            // 네이티브 텍스트 선택/드래그 액션모드가 뜨는 경우가 있어 이중으로 막는다.
            // (setOnLongClickListener를 등록하면 View가 longClickable을 자동으로 true로
            // 되돌리므로, 별도로 isLongClickable=false를 먼저 줄 필요는 없다.)
            setOnLongClickListener { true }
            isHapticFeedbackEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    applyInsetVars()
                }

                // 앱 내 모든 외부 링크는 SceneGetsBridge.openUrl()로 외부 브라우저를 띄우는
                // 방식이라 WebView가 스스로 다른 도메인으로 이동할 일은 원래 없어야 한다.
                // 혹시라도(예상치 못한 리다이렉트 등) 이동 시도가 들어오면 여기서 차단해서,
                // JS 인터페이스가 붙은 상태로 외부 콘텐츠가 로드되는 것을 막는 안전장치.
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

        // WebView를 시스템 바까지 확장한 뒤, 실제 콘텐츠 뷰의 상/하 margin을
        // WindowInsets로 잘라낸다. 이 방식은 Android 기본 앱들이 edge-to-edge에서
        // 사용하는 패턴과 동일하며, WebView 내부 CSS와 무관하게 겹침을 차단한다.
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

    /** 상태바/내비게이션바 색과 아이콘 명암을 현재 테마에 맞춘다. */
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
        // 시스템 바 여백은 WebView padding으로 처리한다. HTML에서 별도로 더하면
        // 상태바 여백이 두 번 적용되므로 항상 0으로 유지한다.
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

    /**
     * 뉴스/검색 링크를 완전히 별도의 브라우저 앱 대신 Chrome Custom Tabs로 연다.
     * 앱 테마 색이 적용된 채로 부드럽게 열리고, 뒤로가기로 앱에 자연스럽게 복귀한다.
     * Custom Tabs를 지원하는 브라우저가 없으면 일반 ACTION_VIEW로 폴백한다.
     */
    private fun openInCustomTab(uri: Uri) {
        val dark = isDarkTheme()
        val toolbarColor = if (dark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val accentColor = if (dark) Color.parseColor("#7D7AFF") else Color.parseColor("#5E5CE6")
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .setNavigationBarColor(toolbarColor)
            .setSecondaryToolbarColor(accentColor)
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorParams)
            .setColorScheme(
                if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT
            )
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
        val opened = runCatching { customTabsIntent.launchUrl(this, uri) }.isSuccess
        if (!opened) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }

    private fun sendData(
        chart: ChartResponse,
        news: NewsResponse,
        schedule: List<ScheduleEvent>,
        chartFetchedAt: Long,
        newsFetchedAt: Long,
        scheduleFetchedAt: Long,
        chartError: Boolean,
        newsError: Boolean,
        scheduleError: Boolean
    ) {
        // Build one JSON object and quote it as a JavaScript string argument.
        val actual = "{" +
            "\"chart\":" + json.encodeToString(chart) + "," +
            "\"news\":" + json.encodeToString(news) + "," +
            "\"schedule\":{" + "\"events\":" + json.encodeToString(schedule) + "}," +
            "\"chartFetchedAt\":" + chartFetchedAt + "," +
            "\"newsFetchedAt\":" + newsFetchedAt + "," +
            "\"scheduleFetchedAt\":" + scheduleFetchedAt + "," +
            "\"chartError\":" + chartError + "," +
            "\"newsError\":" + newsError + "," +
            "\"scheduleError\":" + scheduleError +
            "}"
        val quoted = JSONObject.quote(actual)
        webView.post {
            if (!isFinishing && !isDestroyed) {
                webView.evaluateJavascript("window.SceneGetsWeb && window.SceneGetsWeb.onData($quoted);", null)
            }
        }
    }

    /** 섹션별로 실패(Failed) 또는 갱신 실패 후 캐시로 대체된 상태(isStale)인지 판단한다. */
    private fun isErrorState(state: WidgetState<*>): Boolean =
        state is WidgetState.Failed || (state is WidgetState.Loaded<*> && state.isStale)

    /** 세 섹션의 상태를 조합해서 WebView로 한 번에 전달한다. */
    private fun sendCombined(
        chartState: WidgetState<ChartResponse>,
        newsState: WidgetState<NewsResponse>,
        scheduleState: WidgetState<List<ScheduleEvent>>
    ) {
        val chart = (chartState as? WidgetState.Loaded)?.data ?: ChartResponse()
        val news = (newsState as? WidgetState.Loaded)?.data ?: NewsResponse()
        val schedule = MemberBirthdays.mergeInto((scheduleState as? WidgetState.Loaded)?.data ?: emptyList())

        sendData(
            chart, news, schedule,
            chartFetchedAt = (chartState as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            newsFetchedAt = (newsState as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            scheduleFetchedAt = (scheduleState as? WidgetState.Loaded)?.fetchedAt ?: 0L,
            chartError = isErrorState(chartState),
            newsError = isErrorState(newsState),
            scheduleError = isErrorState(scheduleState)
        )
    }

    private suspend fun loadAll() {
        sendCombined(chartRepository.refresh(), newsRepository.refresh(), scheduleRepository.refresh())
    }

    /** 캐시에 저장된 이전 데이터를 네트워크 요청 없이 그대로 보여준다. */
    private suspend fun loadCacheOnly() {
        sendCombined(
            chartRepository.cachedOrLoading(),
            newsRepository.cachedOrLoading(),
            scheduleRepository.cachedOrLoading()
        )
    }

    /**
     * 특정 섹션만 다시 네트워크에서 새로고침하고, 나머지 섹션은 캐시 값을 그대로 사용해
     * 세 섹션을 다시 조합해서 보낸다. 실패 배너의 "다시 시도" 버튼에서 사용한다.
     */
    private suspend fun refreshSection(chart: Boolean = false, news: Boolean = false, schedule: Boolean = false) {
        val chartState = if (chart) chartRepository.refresh() else chartRepository.cachedOrLoading()
        val newsState = if (news) newsRepository.refresh() else newsRepository.cachedOrLoading()
        val scheduleState = if (schedule) scheduleRepository.refresh() else scheduleRepository.cachedOrLoading()
        sendCombined(chartState, newsState, scheduleState)
    }

    private inner class SceneGetsBridge {
        @JavascriptInterface
        fun refreshAll() {
            scope.launch {
                loadAll()
            }
        }

        @JavascriptInterface
        fun refreshChart() {
            scope.launch {
                refreshSection(chart = true)
            }
        }

        @JavascriptInterface
        fun refreshNews() {
            scope.launch {
                refreshSection(news = true)
            }
        }

        @JavascriptInterface
        fun refreshSchedule() {
            scope.launch {
                refreshSection(schedule = true)
            }
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
            // 반드시 먼저 저장/갱신한 뒤 시스템 테마를 다시 계산한다.
            // 이전에는 다크 선택 상태에서 '시스템'을 누르면 JS가 아직 themeMode=dark인
            // 순간에 getResolvedTheme()을 호출해서 계속 다크로 남는 문제가 있었다.
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
            runOnUiThread { openInCustomTab(uri) }
        }

        @JavascriptInterface
        fun openEmail(address: String?) {
            val to = address?.takeIf { it.isNotBlank() } ?: return
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            }
            runCatching { startActivity(intent) }
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
        const val THUMBNAIL_MAX_DP = 56
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    }
}
