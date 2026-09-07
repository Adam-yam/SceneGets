package com.adamyam.scenegets.widget.chart

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.ChartRank
import com.adamyam.scenegets.models.ChartResponse
import com.adamyam.scenegets.models.ChartSong
import com.adamyam.scenegets.widget.common.PlatformOrder
import com.adamyam.scenegets.widget.common.WidgetCard
import com.adamyam.scenegets.widget.common.WidgetCenterMessage
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.WidgetDivider
import com.adamyam.scenegets.widget.common.WidgetHeader
import com.adamyam.scenegets.widget.common.freshnessLabel
import com.adamyam.scenegets.widget.common.stripHiddenTags
private val WIDE_LAYOUT_MIN_WIDTH = 320.dp
private val PLATFORM_CHIP_WIDTH = 56.dp
private val PLATFORM_CHIP_SPACING = 4.dp
private val TITLE_COLUMN_WIDE_WIDTH = 84.dp

@Composable
fun ChartWidgetContent(state: WidgetState<ChartResponse>, albumImages: Map<String, Bitmap> = emptyMap()) {
    WidgetCard {
        WidgetHeader(
            title = "차트",
            freshness = freshnessLabel(state),
            refreshAction = actionRunCallback<RefreshChartAction>()
        )
        Spacer(modifier = GlanceModifier.height(6.dp))

        when (state) {
            is WidgetState.Loading -> WidgetCenterMessage("차트를 불러오는 중...")
            is WidgetState.Failed -> WidgetCenterMessage("차트를 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val songs = state.data.songs
                if (songs.isEmpty()) {
                    WidgetCenterMessage("표시할 차트 데이터가 없어요")
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        itemsIndexed(songs) { index, song ->
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                if (index > 0) WidgetDivider()
                                SongRow(song, albumImages[song.albumImageUrl])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: ChartSong, albumImage: Bitmap?) {
    val widgetWidth = LocalSize.current.width
    val isWideLayout = widgetWidth >= WIDE_LAYOUT_MIN_WIDTH

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        AlbumCover(albumImage)
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(
            modifier = if (isWideLayout) {
                GlanceModifier.width(TITLE_COLUMN_WIDE_WIDTH)
            } else {
                GlanceModifier.defaultWeight()
            }
        ) {
            Text(
                text = stripHiddenTags(song.songName),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                text = stripHiddenTags(song.artistName),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.5.sp)
            )
            if (!isWideLayout) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                PlatformRanks(song.ranks, chunkSize = 4, fillWidth = true)
            }
        }
        if (isWideLayout) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            PlatformRanks(
                ranks = song.ranks,
                chunkSize = sideChipsPerRow(widgetWidth),
                fillWidth = false
            )
        }
    }
}

private fun sideChipsPerRow(widgetWidth: Dp): Int {
    val reserved = 24.dp + 42.dp + 10.dp + TITLE_COLUMN_WIDE_WIDTH + 8.dp
    val available = widgetWidth - reserved
    val approxChipWidth = PLATFORM_CHIP_WIDTH + PLATFORM_CHIP_SPACING
    val count = (available / approxChipWidth).toInt()
    return count.coerceAtLeast(1)
}

@Composable
private fun PlatformRanks(
    ranks: Map<String, ChartRank>,
    chunkSize: Int,
    fillWidth: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val entries = PlatformOrder.sort(ranks)
    val rows = entries.chunked(chunkSize.coerceAtLeast(1))
    Column(modifier = if (fillWidth) modifier.fillMaxWidth() else modifier) {
        rows.forEach { rowEntries ->
            val rowModifier = if (fillWidth) {
                GlanceModifier.fillMaxWidth().padding(bottom = PLATFORM_CHIP_SPACING)
            } else {
                GlanceModifier.padding(bottom = PLATFORM_CHIP_SPACING)
            }
            Row(modifier = rowModifier) {
                rowEntries.forEachIndexed { index, (platform, rank) ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(PLATFORM_CHIP_SPACING))
                    PlatformChip(platform, rank, GlanceModifier.width(PLATFORM_CHIP_WIDTH))
                }
            }
        }
    }
}

@Composable
private fun AlbumCover(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier
                .size(42.dp)
                .cornerRadius(8.dp)
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(42.dp)
                .cornerRadius(8.dp)
                .background(WidgetColors.surfaceVariant)
        ) {}
    }
}

@Composable
private fun PlatformChip(platform: String, rank: ChartRank, modifier: GlanceModifier = GlanceModifier.width(PLATFORM_CHIP_WIDTH)) {
    val diff = rank.previousRank?.let { it - rank.rank }
    val (changeText, changeColor) = when {
        rank.previousRank == null -> "NEW" to WidgetColors.flat
        diff != null && diff > 0 -> "▲$diff" to WidgetColors.up
        diff != null && diff < 0 -> "▼${-diff}" to WidgetColors.down
        else -> "-" to WidgetColors.flat
    }
    Column(
        modifier = modifier
            .background(WidgetColors.surfaceVariant)
            .cornerRadius(8.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = PlatformOrder.label(platform),
            maxLines = 1,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 8.5.sp)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${rank.rank}",
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(3.dp))
            Text(
                text = changeText,
                style = TextStyle(color = changeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
