package com.adamyam.scenegets.widget.news

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
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
import com.adamyam.scenegets.models.NewsArticle
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.freshnessLabel

@Composable
fun NewsWidgetContent(state: WidgetState<NewsResponse>, thumbnails: Map<String, Bitmap> = emptyMap()) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .padding(8.dp)
    ) {
        NewsHeader(state)
        Spacer(modifier = GlanceModifier.height(4.dp))

        when (state) {
            is WidgetState.Loading -> CenterMessage("뉴스를 불러오는 중...")
            is WidgetState.Failed -> CenterMessage("뉴스를 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val articles = state.data.articles
                if (articles.isEmpty()) {
                    CenterMessage("표시할 뉴스가 없어요")
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(articles) { article -> ArticleRow(article, thumbnails[article.thumbnail]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsHeader(state: WidgetState<NewsResponse>) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "뉴스",
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .clickable(actionRunCallback<RefreshNewsAction>())
        )
    }
}

@Composable
private fun ArticleRow(article: NewsArticle, thumbnail: Bitmap?) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.cardBackground)
            .cornerRadius(8.dp)
            .padding(8.dp)
            // 뉴스는 항목을 누르면 해당 기사로 바로 이동
            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url))))
    ) {
        ArticleThumbnail(thumbnail)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = article.title,
                maxLines = 2,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = article.source,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 9.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = article.date,
                    style = TextStyle(color = WidgetColors.textFaint, fontSize = 9.sp)
                )
            }
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

@Composable
private fun ArticleThumbnail(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(6.dp)
        )
    } else {
        // 이미지가 아직 없거나 로드에 실패했을 때의 빈 자리 표시
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(6.dp)
                .background(WidgetColors.chipBackground)
        ) {}
    }
}

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp)
    )
}
