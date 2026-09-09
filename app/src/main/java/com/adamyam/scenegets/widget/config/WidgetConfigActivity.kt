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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_widget_config)
        WindowInsetsControllerCompat(window, window.decorView).apply {
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

            val saved = if (glanceId != null) {
                runCatching {
                    updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                        prefs[WidgetAppearanceKeys.themeMode] = appearance.themeMode.name
                        prefs[WidgetAppearanceKeys.opacityPercent] = appearance.opacityPercent
                    }
                    targetWidget.update(this@WidgetConfigActivity, glanceId)
                }.isSuccess
            } else {
                false
            }
            if (!saved) {
                android.widget.Toast.makeText(
                    this@WidgetConfigActivity,
                    R.string.widget_config_save_failed,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
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
