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
import com.adamyam.scenegets.widget.common.stripHiddenTags

// ChartWidget에 등록된 두 크기(250dp/380dp) 사이의 경계값.
// 이 값 이상으로 넓어지면 옆으로 늘어난 것으로 보고 레이아웃을 바꾼다.
private val WIDE_LAYOUT_MIN_WIDTH = 320.dp

// 플랫폼 순위 칩 하나의 고정 폭. 폭을 고정해두면 여러 줄에 걸쳐 칩이 표시될 때
// 세로로 열이 맞춰져서 "표"처럼 한눈에 훑어보기 쉬워진다.
private val PLATFORM_CHIP_WIDTH = 56.dp
private val PLATFORM_CHIP_SPACING = 4.dp

// 와이드 레이아웃에서 제목/아티스트 컬럼의 고정 폭.
// 이전에는 defaultWeight()로 남는 공간을 전부 차지해서 제목이 짧을 때
// 순위 칩이 카드 오른쪽 끝까지 밀려나 보였다. 폭을 고정하면 칩이 제목
// 바로 옆(좌측)부터 붙어서 시작한다.
private val TITLE_COLUMN_WIDE_WIDTH = 84.dp

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
    val widgetWidth = LocalSize.current.width
    val isWideLayout = widgetWidth >= WIDE_LAYOUT_MIN_WIDTH

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(8.dp)
            .padding(6.dp)
    ) {
        AlbumCover(albumImage)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(
            // 와이드 레이아웃에서는 고정 폭을 줘서 순위 칩이 제목 바로 옆(좌측)부터
            // 시작하게 하고, 좁은 레이아웃에서는 기존처럼 가로 전체를 채운다.
            modifier = if (isWideLayout) {
                GlanceModifier.width(TITLE_COLUMN_WIDE_WIDTH)
            } else {
                GlanceModifier.defaultWeight()
            }
        ) {
            Text(
                text = stripHiddenTags(song.songName),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = stripHiddenTags(song.artistName),
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
            // 순위 영역이 제목 컬럼 바로 옆(좌측)에 붙어서 시작하고, 실제로
            // 들어갈 수 있는 만큼만 한 줄에 채운 뒤 넘치는 나머지는 화면 밖으로
            // 잘리는 대신 다음 줄로 자연스럽게 넘어가게 한다.
            PlatformRanks(song.ranks, chunkSize = sideChipsPerRow(widgetWidth), fillWidth = false)
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

/**
 * 옆 순위 영역에 실제로 들어갈 수 있는 칩 개수를 위젯 폭 기준으로 대략 추정한다.
 * (카드 패딩 + 앨범 커버 + 제목/아티스트 컬럼이 최소로 필요로 하는 폭을 뺀 나머지를
 * 칩 하나의 평균 폭으로 나눈 값. 넉넉하게 잡아서 칩이 잘려나가는 것을 막는다.)
 */
private fun sideChipsPerRow(widgetWidth: Dp): Int {
    // 카드 패딩(6*2) + 앨범 커버 + 스페이서 + 제목 컬럼 고정폭 + 스페이서
    val reserved = 12.dp + 40.dp + 8.dp + TITLE_COLUMN_WIDE_WIDTH + 8.dp
    val available = widgetWidth - reserved
    val approxChipWidth = PLATFORM_CHIP_WIDTH + PLATFORM_CHIP_SPACING
    val count = (available / approxChipWidth).toInt()
    return count.coerceAtLeast(1)
}

@Composable
private fun PlatformRanks(ranks: Map<String, ChartRank>, chunkSize: Int, fillWidth: Boolean) {
    // 실제 순위가 있는 플랫폼만 chunkSize개씩 줄바꿈해서 전부 표시 (숨김/탭 없음).
    // 칩 폭을 고정해서 여러 줄이 생겨도 세로로 열이 맞춰지는 표 형태가 되도록 한다.
    val entries = PlatformOrder.sort(ranks)
    Column {
        entries.chunked(chunkSize).forEach { rowEntries ->
            val rowModifier = if (fillWidth) {
                GlanceModifier.fillMaxWidth().padding(bottom = PLATFORM_CHIP_SPACING)
            } else {
                GlanceModifier.padding(bottom = PLATFORM_CHIP_SPACING)
            }
            Row(modifier = rowModifier) {
                rowEntries.forEachIndexed { index, (platform, rank) ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(PLATFORM_CHIP_SPACING))
                    PlatformChip(platform, rank)
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
    val (changeText, changeColor) = when {
        rank.previousRank == null -> "NEW" to WidgetColors.flat
        diff != null && diff > 0 -> "▲$diff" to WidgetColors.up
        diff != null && diff < 0 -> "▼${-diff}" to WidgetColors.down
        else -> "-" to WidgetColors.flat
    }

    // 플랫폼명(위) / 순위+증감(아래) 2줄 구성 + 고정 폭으로,
    // 여러 개를 나열해도 표처럼 정렬되어 한눈에 훑어보기 쉽게 만든다.
    Column(
        modifier = GlanceModifier
            .width(PLATFORM_CHIP_WIDTH)
            .background(WidgetColors.chipBackground)
            .cornerRadius(8.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = PlatformOrder.label(platform),
            maxLines = 1,
            style = TextStyle(color = WidgetColors.textSecondary, fontSize = 8.sp)
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

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
