package com.adamyam.scenegets.widget.chart

import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.R
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ChartRank
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.models.ChartSong
import com.adamyam.scenegets.widget.common.PlatformOrder
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.freshnessLabel

@Composable
fun ChartWidgetContent(state: WidgetState<ChartResponse>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .padding(8.dp)
    ) {
        ChartHeader(state)
        Spacer(modifier = GlanceModifier.height(4.dp))

        when (state) {
            is WidgetState.Loading -> CenterMessage("차트를 불러오는 중...")
            is WidgetState.Failed -> CenterMessage("차트를 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val songs = state.data.songs
                if (songs.isEmpty()) {
                    CenterMessage("표시할 차트 데이터가 없어요")
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(songs) { song -> SongRow(song) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(state: WidgetState<ChartResponse>) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "차트",
            style = TextStyle(
                color = WidgetColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = freshnessLabel(state),
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 9.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier
                .size(16.dp)
                .clickable(actionRunCallback<RefreshChartAction>())
        )
    }
}

@Composable
private fun SongRow(song: ChartSong) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(8.dp)
            .padding(6.dp)
    ) {
        Text(
            text = song.songName,
            maxLines = 1,
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = song.artistName,
            maxLines = 1,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.height(3.dp))

        // 실제 순위가 있는 플랫폼만 4개씩 줄바꿈해서 전부 표시 (숨김/탭 없음)
        val entries = PlatformOrder.sort(song.ranks)
        entries.chunked(4).forEach { rowEntries ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
                rowEntries.forEach { (platform, rank) ->
                    PlatformChip(platform, rank)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

@Composable
private fun PlatformChip(platform: String, rank: ChartRank) {
    val diff = rank.previousRank?.let { it - rank.rank }
    val (arrowText, arrowColor) = when {
        rank.previousRank == null -> "NEW" to WidgetColors.flat
        diff != null && diff > 0 -> "▲$diff" to WidgetColors.up
        diff != null && diff < 0 -> "▼${-diff}" to WidgetColors.down
        else -> "-" to WidgetColors.flat
    }

    Row(
        modifier = GlanceModifier
            .background(WidgetColors.chipBackground)
            .cornerRadius(6.dp)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = PlatformOrder.label(platform),
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 8.sp)
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(
            text = "${rank.rank}",
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(
            text = arrowText,
            style = TextStyle(color = arrowColor, fontSize = 8.sp)
        )
    }
}

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
