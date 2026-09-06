package com.adamyam.scenegets.widget.news

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adamyam.scenegets.data.WidgetState
import com.adamyam.scenegets.models.NewsArticle
import com.adamyam.scenegets.models.NewsResponse
import com.adamyam.scenegets.widget.common.WidgetCard
import com.adamyam.scenegets.widget.common.WidgetCenterMessage
import com.adamyam.scenegets.widget.common.WidgetColors
import com.adamyam.scenegets.widget.common.WidgetDivider
import com.adamyam.scenegets.widget.common.WidgetHeader
import com.adamyam.scenegets.widget.common.freshnessLabel

@Composable
fun NewsWidgetContent(state: WidgetState<NewsResponse>, thumbnails: Map<String, Bitmap> = emptyMap()) {
    WidgetCard {
        WidgetHeader(
            title = "뉴스",
            accentColor = WidgetColors.down,
            freshness = freshnessLabel(state),
            refreshAction = actionRunCallback<RefreshNewsAction>()
        )

        when (state) {
            is WidgetState.Loading -> WidgetCenterMessage("뉴스를 불러오는 중...")
            is WidgetState.Failed -> WidgetCenterMessage("뉴스를 불러오지 못했어요\n${state.message}")
            is WidgetState.Loaded -> {
                val articles = state.data.articles
                if (articles.isEmpty()) {
                    WidgetCenterMessage("표시할 뉴스가 없어요")
                } else {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        itemsIndexed(articles) { index, article ->
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                if (index > 0) WidgetDivider()
                                ArticleRow(article, thumbnails[article.thumbnail])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(article: NewsArticle, thumbnail: Bitmap?) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url))))
    ) {
        ArticleThumbnail(thumbnail, article.source)
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = article.title,
                maxLines = 2,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = article.source,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = article.date,
                    style = TextStyle(color = WidgetColors.textFaint, fontSize = 10.5.sp)
                )
            }
        }
    }
}

@Composable
private fun ArticleThumbnail(bitmap: Bitmap?, source: String) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(10.dp)
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(10.dp)
                .background(WidgetColors.accentChipBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = source.take(1).ifBlank { "N" },
                style = TextStyle(color = WidgetColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
