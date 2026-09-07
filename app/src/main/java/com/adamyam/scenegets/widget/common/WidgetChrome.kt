package com.adamyam.scenegets.widget.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.R

private val OUTER_RADIUS = 20.dp
private val INNER_RADIUS = 19.dp
private val BORDER_WIDTH = 1.dp

@Composable
fun WidgetCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.stroke)
            .cornerRadius(OUTER_RADIUS)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(BORDER_WIDTH)
                .background(WidgetColors.surface)
                .cornerRadius(INNER_RADIUS)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content
        )
    }
}

@Composable
fun WidgetHeader(
    title: String,
    freshness: String,
    refreshAction: Action
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = freshness,
            style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        WidgetRefreshButton(refreshAction)
    }
}

@Composable
private fun WidgetRefreshButton(action: Action) {
    Box(
        modifier = GlanceModifier
            .size(24.dp)
            .cornerRadius(12.dp)
            .background(WidgetColors.surfaceVariant)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier.size(13.dp)
        )
    }
}

@Composable
fun WidgetDivider() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WidgetColors.divider)
    ) {}
}

@Composable
fun WidgetCenterMessage(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        )
    }
}
