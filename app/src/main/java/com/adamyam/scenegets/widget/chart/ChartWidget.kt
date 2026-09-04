package com.adamyam.scenegets.widget.chart

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.adamyam.scenegets.data.ChartRepository
import com.adamyam.scenegets.data.ImageCache
import com.adamyam.scenegets.data.WidgetState

class ChartWidget : GlanceAppWidget() {
    // 위젯을 옆으로 늘렸을 때 레이아웃이 반응할 수 있도록 두 가지 크기를 등록해둔다.
    // 실제 위젯 크기는 이 중 더 가까운 쪽으로 매핑되어 ChartWidgetContent의 LocalSize에 전달된다.
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 110.dp), // 기본 크기
            DpSize(380.dp, 110.dp)  // 옆으로 늘린 크기
        )
    )

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
