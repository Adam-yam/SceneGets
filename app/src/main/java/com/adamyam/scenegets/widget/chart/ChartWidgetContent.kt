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
import kotlin.math.ceil

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
            // 순위 영역이 제목 컬럼 바로 옆(좌측)에서 시작하고, 남은 폭 전체를
            // 차지하도록(defaultWeight) 해서 위젯을 옆으로 넓힐수록 칩들이 그
            // 늘어난 만큼 함께 커지며 채운다 (빈 공간으로 남지 않음).
            PlatformRanks(
                ranks = song.ranks,
                chunkSize = sideChipsPerRow(widgetWidth),
                fillWidth = true,
                flexibleChipWidth = true,
                modifier = GlanceModifier.defaultWeight()
            )
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
private fun PlatformRanks(
    ranks: Map<String, ChartRank>,
    chunkSize: Int,
    fillWidth: Boolean,
    flexibleChipWidth: Boolean = false,
    modifier: GlanceModifier = GlanceModifier
) {
    // 실제 순위가 있는 플랫폼만 표시한다 (숨김/탭 없음).
    // flexibleChipWidth가 true면(와이드 레이아웃) 칩 폭을 고정하지 않고 한 줄 안에서
    // 균등하게 늘어나게 해서, 위젯을 넓힐수록 칩도 함께 커지며 남는 공간을 채운다.
    // 마지막 줄만 개수가 적어 칩이 비정상적으로 커지는 걸 막기 위해 줄 인원을
    // 최대한 균형있게 나눈다(balancedRowSizes).
    val entries = PlatformOrder.sort(ranks)
    val rows = if (flexibleChipWidth) {
        splitByRowSizes(entries, balancedRowSizes(entries.size, chunkSize))
    } else {
        entries.chunked(chunkSize)
    }
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
                    val chipModifier = if (flexibleChipWidth) {
                        GlanceModifier.defaultWeight()
                    } else {
                        GlanceModifier.width(PLATFORM_CHIP_WIDTH)
                    }
                    PlatformChip(platform, rank, chipModifier)
                }
            }
        }
    }
}

/**
 * total개 항목을 한 줄에 최대 maxPerRow개까지 담되, 줄 수를 정한 뒤 각 줄의 인원을
 * 최대한 고르게 분배한다. 예) total=7, maxPerRow=4 → 4,3이 아니라 rows=2로 정해지므로
 * 4,3 그대로; total=7, maxPerRow=3 → 단순 chunked면 3,3,1(마지막 줄 1개가 과도하게
 * 늘어남)이 되지만, 이 함수는 rows=3, 3,2,2로 고르게 나눠 준다.
 */
private fun balancedRowSizes(total: Int, maxPerRow: Int): List<Int> {
    if (total <= 0) return emptyList()
    val safeMaxPerRow = maxPerRow.coerceAtLeast(1)
    val rowCount = ceil(total.toDouble() / safeMaxPerRow).toInt().coerceAtLeast(1)
    val base = total / rowCount
    val remainder = total % rowCount
    return (0 until rowCount).map { index -> if (index < remainder) base + 1 else base }
}

private fun <T> splitByRowSizes(list: List<T>, sizes: List<Int>): List<List<T>> {
    var startIndex = 0
    return sizes.map { size ->
        val chunk = list.subList(startIndex, startIndex + size)
        startIndex += size
        chunk
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
private fun PlatformChip(platform: String, rank: ChartRank, modifier: GlanceModifier = GlanceModifier.width(PLATFORM_CHIP_WIDTH)) {
    val diff = rank.previousRank?.let { it - rank.rank }
    val (changeText, changeColor) = when {
        rank.previousRank == null -> "NEW" to WidgetColors.flat
        diff != null && diff > 0 -> "▲$diff" to WidgetColors.up
        diff != null && diff < 0 -> "▼${-diff}" to WidgetColors.down
        else -> "-" to WidgetColors.flat
    }

    // 플랫폼명(위) / 순위+증감(아래) 2줄 구성.
    // 좁은 레이아웃/기본 상태에서는 고정 폭으로 표처럼 세로 정렬되고,
    // 와이드 레이아웃에서는 modifier로 넘어오는 defaultWeight()가 적용되어
    // 한 줄 안에서 균등하게 늘어난다.
    Column(
        modifier = modifier
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
