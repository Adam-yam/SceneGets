package com.adamyam.scenegets.widget.chart

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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

// ChartWidget에 등록된 두 크기(250dp/380dp) 사이의 경계값.
// 이 값 이상으로 넓어지면 옆으로 늘어난 것으로 보고 레이아웃을 바꾼다.
private val WIDE_LAYOUT_MIN_WIDTH = 320.dp

@Composable
fun ChartWidgetContent(state: WidgetState<ChartResponse>, albumImages: Map<String, Bitmap> = emptyMap()) {
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
                        items(songs) { song -> SongRow(song, albumImages[song.albumImageUrl]) }
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
private fun SongRow(song: ChartSong, albumImage: Bitmap?) {
    // 위젯을 옆으로 늘렸을 때(ChartWidget의 SizeMode.Responsive 참고)만
    // 플랫폼 순위를 제목 아래가 아니라 옆으로 따로 빼서 보여준다.
    val isWideLayout = LocalSize.current.width >= WIDE_LAYOUT_MIN_WIDTH

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(8.dp)
            .padding(6.dp)
    ) {
        AlbumCover(albumImage)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
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
            if (!isWideLayout) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                PlatformRanks(song.ranks, chunkSize = 4, fillWidth = true)
            }
        }
        if (isWideLayout) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            PlatformRanks(song.ranks, chunkSize = 2, fillWidth = false)
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

@Composable
private fun PlatformRanks(ranks: Map<String, ChartRank>, chunkSize: Int, fillWidth: Boolean) {
    // 실제 순위가 있는 플랫폼만 chunkSize개씩 줄바꿈해서 전부 표시 (숨김/탭 없음)
    val entries = PlatformOrder.sort(ranks)
    Column {
        entries.chunked(chunkSize).forEach { rowEntries ->
            val rowModifier = if (fillWidth) {
                GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)
            } else {
                GlanceModifier.padding(bottom = 2.dp)
            }
            Row(modifier = rowModifier) {
                rowEntries.forEach { (platform, rank) ->
                    PlatformChip(platform, rank)
                    Spacer(modifier = GlanceModifier.width(4.dp))
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
                .size(40.dp)
                .cornerRadius(6.dp)
        )
    } else {
        // 이미지가 아직 없거나 로드에 실패했을 때의 빈 자리 표시
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .cornerRadius(6.dp)
                .background(WidgetColors.chipBackground)
        ) {}
    }
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
