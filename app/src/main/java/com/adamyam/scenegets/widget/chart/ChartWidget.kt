package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.WidgetState

class ChartWidget : GlanceAppWidget() {
    // 이전에는 SizeMode.Responsive로 딱 두 크기(250dp/380dp)만 등록해뒀는데,
    // 실제 위젯이 그보다 더 넓어져도 항상 가장 가까운(최대) 380dp 기준으로만
    // 계산되어서 위젯을 아무리 늘려도 칩 개수가 늘어나지 않는 문제가 있었다.
    // SizeMode.Exact로 바꾸면 LocalSize.current가 실제 위젯 폭을 그대로 전달하므로,
    // 위젯을 넓힐수록 ChartWidgetContent의 sideChipsPerRow 계산이 계속 더 큰
    // 값을 내놓고, 그만큼 한 줄에 표시되는 순위 칩 개수도 계속 늘어난다.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 네트워크 호출 없이 캐시부터 즉시 보여주고, 실제 갱신은 WorkManager/RefreshAction이 담당
        val state = ChartRepository(context).cachedOrLoading()

        // Composable 안에서는 네트워크를 탈 수 없으므로, 앨범 커버는 여기서 미리 받아둔다.
        val albumImages = if (state is WidgetState.Loaded) {
            ImageCache.loadAll(context, state.data.songs.mapNotNull { it.albumImageUrl })
        } else {
            emptyMap()
        }

        provideContent {
            ChartWidgetContent(state, albumImages)
        }
    }
}
