package com.adamyam.scenegets.widget.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.adamyam.scenegets.R
import com.adamyam.scenegets.widget.chart.ChartWidget
import com.adamyam.scenegets.widget.chart.ChartWidgetReceiver
import com.adamyam.scenegets.widget.common.WidgetAppearance
import com.adamyam.scenegets.widget.common.WidgetAppearanceKeys
import com.adamyam.scenegets.widget.common.WidgetThemeMode
import com.adamyam.scenegets.widget.news.NewsWidget
import com.adamyam.scenegets.widget.news.NewsWidgetReceiver
import com.adamyam.scenegets.widget.schedule.ScheduleWidget
import com.adamyam.scenegets.widget.schedule.ScheduleWidgetReceiver
import com.adamyam.scenegets.work.WidgetWorkScheduler
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 갤럭시(등)에서 위젯을 길게 눌러 홈 화면에 추가할 때 자동으로 뜨는 구성(configure) 화면.
 * AndroidManifest.xml의 APPWIDGET_CONFIGURE 인텐트 필터 + 각 *_widget_info.xml의
 * android:configure 속성으로 시스템이 배치 직전에 이 액티비티를 띄워준다.
 *
 * 차트/뉴스/스케줄 위젯 3종 모두 이 화면 하나를 공용으로 쓰고, appWidgetId로부터
 * 어떤 위젯인지(및 어떤 GlanceAppWidget 인스턴스로 갱신해야 하는지)를 역으로 찾는다.
 *
 * 이미 추가된 위젯을 "위젯 설정"으로 재구성하는 경우(런처가 지원 시)에도 같은 화면이
 * 다시 뜨는데, 그때는 기존에 저장된 값을 미리 불러와 보여준다.
 */
class WidgetConfigActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var targetWidget: GlanceAppWidget
    private lateinit var triggerDataRefresh: (android.content.Context) -> Unit

    private lateinit var previewCardBg: View
    private lateinit var previewHeaderText: TextView
    private lateinit var previewBodyText: TextView
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var opacityValueText: TextView
    private lateinit var themeRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자가 완료를 누르기 전에 뒤로가기 등으로 나가면 위젯 추가 자체가 취소되도록
        // 기본 결과를 먼저 CANCELED로 깔아둔다 (Android 위젯 구성 화면의 표준 패턴).
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resolved = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            resolveWidget(appWidgetId)
        } else {
            null
        }

        if (resolved == null) {
            finish()
            return
        }
        targetWidget = resolved.first
        triggerDataRefresh = resolved.third

        // Android 15+ (targetSdk 35+)에서는 edge-to-edge가 기본 적용된다.
        // 시스템 바 영역까지 그리되, 실제 설정 카드는 system bar inset 안쪽에서
        // 중앙에 배치해 Galaxy/제스처·3버튼 내비게이션 모두에서 겹침이 없도록 한다.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_widget_config)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // 전체 화면 scrim이 어두우므로 상태/내비게이션 바 아이콘은 항상 밝게 표시한다.
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val configRoot = findViewById<View>(R.id.configRoot)
        ViewCompat.setOnApplyWindowInsetsListener(configRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(configRoot)

        previewCardBg = findViewById(R.id.previewCardBg)
        previewHeaderText = findViewById(R.id.previewHeaderText)
        previewBodyText = findViewById(R.id.previewBodyText)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        opacityValueText = findViewById(R.id.opacityValueText)
        themeRadioGroup = findViewById(R.id.themeRadioGroup)

        // previewContainer의 체크무늬 배경(BitmapDrawable)은 스스로 둥근 모서리 아웃라인을
        // 계산해주지 않으므로, previewCardBg와 같은 반경(20dp)으로 직접 잘라낸다.
        // 그래야 알파가 낮아졌을 때 체크무늬가 previewCardBg의 둥근 모서리 밖으로
        // 각지게 삐져나오지 않는다.
        val previewContainer = findViewById<View>(R.id.previewContainer)
        val previewCornerRadiusPx = 20f * resources.displayMetrics.density
        previewContainer.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, previewCornerRadiusPx)
            }
        }
        previewContainer.clipToOutline = true

        findViewById<TextView>(R.id.configTitleText).text =
            getString(R.string.widget_config_title, resolved.second)

        opacitySeekBar.max = WidgetAppearance.MAX_OPACITY_PERCENT
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateOpacityLabel(progress)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        themeRadioGroup.setOnCheckedChangeListener { _, _ -> updatePreview() }

        findViewById<View>(R.id.cancelButton).setOnClickListener { finish() }
        findViewById<View>(R.id.confirmButton).setOnClickListener { saveAndFinish() }

        loadExistingAppearance()
    }

    /**
     * appWidgetId가 어느 위젯 종류인지(provider 클래스명 기준) 역으로 찾아낸다.
     * 세 번째 값은 "완료" 시 최신 데이터를 즉시 한 번 받아오는 함수 — 구성 화면이 있는
     * 위젯은 최초 추가 시 시스템이 onUpdate()를 자동으로 호출해주지 않으므로
     * (그건 ChartWidgetReceiver 등에서 WidgetWorkScheduler.refreshXxxNow를 트리거하는데),
     * 여기서 같은 트리거를 대신 걸어줘야 예전처럼 위젯을 추가하자마자 최신 데이터가 뜬다.
     */
    private fun resolveWidget(id: Int): Triple<GlanceAppWidget, String, (android.content.Context) -> Unit>? {
        val providerInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(id) ?: return null
        return when (providerInfo.provider) {
            ComponentName(this, ChartWidgetReceiver::class.java) ->
                Triple(ChartWidget(), getString(R.string.widget_chart_label), WidgetWorkScheduler::refreshChartNow)
            ComponentName(this, NewsWidgetReceiver::class.java) ->
                Triple(NewsWidget(), getString(R.string.widget_news_label), WidgetWorkScheduler::refreshNewsNow)
            ComponentName(this, ScheduleWidgetReceiver::class.java) ->
                Triple(ScheduleWidget(), getString(R.string.widget_schedule_label), WidgetWorkScheduler::refreshScheduleNow)
            else -> null
        }
    }

    private fun loadExistingAppearance() {
        scope.launch {
            val glanceId = runCatching {
                GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            }.getOrNull()

            val appearance = glanceId?.let {
                runCatching {
                    val prefs = getAppWidgetState(
                        this@WidgetConfigActivity,
                        PreferencesGlanceStateDefinition,
                        it
                    )
                    WidgetAppearance(
                        themeMode = WidgetThemeMode.fromStorageValue(prefs[WidgetAppearanceKeys.themeMode]),
                        opacityPercent = prefs[WidgetAppearanceKeys.opacityPercent]
                            ?: WidgetAppearance.DEFAULT_OPACITY_PERCENT
                    )
                }.getOrNull()
            } ?: WidgetAppearance()

            opacitySeekBar.progress = appearance.opacityPercent
            updateOpacityLabel(appearance.opacityPercent)
            themeRadioGroup.check(
                when (appearance.themeMode) {
                    WidgetThemeMode.SYSTEM -> R.id.radioThemeSystem
                    WidgetThemeMode.LIGHT -> R.id.radioThemeLight
                    WidgetThemeMode.DARK -> R.id.radioThemeDark
                }
            )
            updatePreview()
        }
    }

    private fun updateOpacityLabel(progress: Int) {
        opacityValueText.text = getString(R.string.widget_config_opacity_value, progress)
    }

    private fun currentSelectedThemeMode(): WidgetThemeMode = when (themeRadioGroup.checkedRadioButtonId) {
        R.id.radioThemeLight -> WidgetThemeMode.LIGHT
        R.id.radioThemeDark -> WidgetThemeMode.DARK
        else -> WidgetThemeMode.SYSTEM
    }

    /** 실제 위젯 색 팔레트(WidgetTheme.kt)와 같은 값을 여기서도 그대로 써서 미리보기를 맞춘다. */
    private fun updatePreview() {
        val isDark = when (currentSelectedThemeMode()) {
            WidgetThemeMode.SYSTEM ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            WidgetThemeMode.LIGHT -> false
            WidgetThemeMode.DARK -> true
        }
        val opacityFraction = opacitySeekBar.progress / 100f

        val surfaceColor = if (isDark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
        val strokeColor = if (isDark) 0x12FFFFFF else 0x1A3C3C43
        val textPrimaryColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt()
        val textSecondaryColor = if (isDark) 0xFF98989D.toInt() else 0xFF6C6C70.toInt()

        val density = resources.displayMetrics.density
        previewCardBg.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * density
            setColor(withAlpha(surfaceColor, opacityFraction))
            setStroke((1f * density).toInt(), withAlpha(strokeColor, opacityFraction))
        }
        // 카드 배경만 투명해지고, 글자 색은 항상 완전 불투명 그대로 유지한다.
        previewHeaderText.setTextColor(textPrimaryColor)
        previewBodyText.setTextColor(textSecondaryColor)
    }

    private fun withAlpha(color: Int, alphaFraction: Float): Int {
        val alpha = (255 * alphaFraction).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun saveAndFinish() {
        val appearance = WidgetAppearance(
            themeMode = currentSelectedThemeMode(),
            opacityPercent = opacitySeekBar.progress
        )

        scope.launch {
            val glanceId = runCatching {
                GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            }.getOrNull()

            if (glanceId != null) {
                runCatching {
                    updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                        prefs[WidgetAppearanceKeys.themeMode] = appearance.themeMode.name
                        prefs[WidgetAppearanceKeys.opacityPercent] = appearance.opacityPercent
                    }
                    // 완료를 누르자마자 반영된 모습을 볼 수 있도록 즉시 갱신.
                    targetWidget.update(this@WidgetConfigActivity, glanceId)
                }
            }

            // 구성 화면이 있는 위젯은 최초 추가 시 onUpdate()가 자동으로 안 불릴 수 있어서,
            // 여기서 데이터 새로고침도 함께 트리거해준다 (재구성 때는 최신 데이터로 갱신되는 효과).
            runCatching { triggerDataRefresh(this@WidgetConfigActivity) }

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
