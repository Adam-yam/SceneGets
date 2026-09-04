package com.adamyam.scenegets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamyam.scenegets.data.*
import com.adamyam.scenegets.models.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

private enum class Tab(val title: String, val subtitle: String, val icon: ImageVector) {
    NEWS("뉴스", "최신 소식", Icons.Filled.Newspaper),
    SCHEDULE("스케줄", "다가오는 일정", Icons.Filled.CalendarMonth),
    CHART("차트", "실시간 순위", Icons.Filled.BarChart),
    SETTINGS("설정", "앱 환경설정", Icons.Filled.Settings)
}

private val Ink = Color(0xFF111114)
private val SecondaryInk = Color(0xFF74747C)
private val Canvas = Color(0xFFF5F5F7)
private val Glass = Color.White.copy(alpha = 0.78f)
private val Hairline = Color.Black.copy(alpha = 0.06f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SceneGetsApp() }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    @Composable
    private fun SceneGetsApp() {
        var tab by remember { mutableStateOf(Tab.NEWS) }
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val newsRepo = remember { NewsRepository(applicationContext) }
        val scheduleRepo = remember { ScheduleRepository(applicationContext) }
        val chartRepo = remember { ChartRepository(applicationContext) }

        var news by remember { mutableStateOf<WidgetState<NewsResponse>>(WidgetState.Loading) }
        var schedule by remember { mutableStateOf<WidgetState<List<ScheduleEvent>>>(WidgetState.Loading) }
        var chart by remember { mutableStateOf<WidgetState<ChartResponse>>(WidgetState.Loading) }

        LaunchedEffect(Unit) {
            news = newsRepo.cachedOrLoading()
            schedule = scheduleRepo.cachedOrLoading()
            chart = chartRepo.cachedOrLoading()
            scope.launch { news = newsRepo.refresh() }
            scope.launch { schedule = scheduleRepo.refresh() }
            scope.launch { chart = chartRepo.refresh() }
        }

        fun refreshAll() {
            if (refreshing) return
            scope.launch {
                refreshing = true
                launch { news = newsRepo.refresh() }
                launch { schedule = scheduleRepo.refresh() }
                launch { chart = chartRepo.refresh() }
                // Child jobs finish before this scope completes.
                kotlinx.coroutines.delay(50)
                refreshing = false
            }
        }

        MaterialTheme(
            colorScheme = lightColorScheme(
                background = Canvas,
                surface = Color.White,
                onBackground = Ink,
                onSurface = Ink,
                primary = Ink
            )
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Canvas, Color(0xFFF0F0F3))
                        )
                    )
            ) {
                Column(Modifier.fillMaxSize()) {
                    AppHeader(tab, refreshing) { refreshAll() }
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition",
                        modifier = Modifier.weight(1f)
                    ) { current ->
                        when (current) {
                            Tab.NEWS -> NewsScreen(news, ::openUrl)
                            Tab.SCHEDULE -> ScheduleScreen(schedule)
                            Tab.CHART -> ChartScreen(chart)
                            Tab.SETTINGS -> SettingsScreen()
                        }
                    }
                }

                FloatingGlassBar(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }

    @Composable
    private fun AppHeader(tab: Tab, refreshing: Boolean, onRefresh: () -> Unit) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 22.dp, end = 18.dp, top = 17.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("SceneGets", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.1).sp)
                Text(tab.subtitle, color = SecondaryInk, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f))
            ) {
                if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, "새로고침", tint = Ink)
            }
        }
    }

    @Composable
    private fun NewsScreen(state: WidgetState<NewsResponse>, onClick: (String) -> Unit) {
        StateContainer(state, "뉴스를 불러오지 못했어요") { data ->
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 132.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionIntro(
                        "TODAY",
                        if (data.articles.isEmpty()) "새로운 뉴스가 없어요" else "${data.articles.size}개의 새로운 소식",
                        "카드를 누르면 원문으로 이동합니다."
                    )
                }
                items(data.articles) { article -> NewsCard(article) { onClick(article.url) } }
                if (data.articles.isEmpty()) item { EmptyCard("표시할 뉴스가 없어요", "새로고침을 눌러 다시 확인해보세요.") }
            }
        }
    }

    @Composable
    private fun NewsCard(article: NewsArticle, onClick: () -> Unit) {
        GlassCard(Modifier.clickable(onClick = onClick)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!article.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = article.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(17.dp))
                    )
                } else {
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFFE9E9ED)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Newspaper, null, tint = SecondaryInk) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        article.source.ifBlank { "NEWS" }.uppercase(Locale.getDefault()),
                        color = SecondaryInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp
                    )
                    Text(
                        article.title, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp)
                    )
                    if (article.date.isNotBlank()) Text(article.date, color = SecondaryInk, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Color.Black.copy(alpha = .28f), modifier = Modifier.size(19.dp))
            }
        }
    }

    @Composable
    private fun ScheduleScreen(state: WidgetState<List<ScheduleEvent>>) {
        StateContainer(state, "스케줄을 불러오지 못했어요") { events ->
            val grouped = remember(events) { events.groupBy { it.date }.toList() }
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 132.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionIntro(
                        "SCHEDULE",
                        if (events.isEmpty()) "예정된 일정 없음" else "다가오는 일정",
                        if (events.isEmpty()) "새 일정이 등록되면 여기에 표시됩니다." else "${events.size}개의 일정을 날짜순으로 정리했어요."
                    )
                }
                grouped.forEach { (date, dayEvents) ->
                    item { DateHeading(date, dayEvents.size) }
                    items(dayEvents) { ScheduleCard(it) }
                }
            }
        }
    }

    @Composable
    private fun DateHeading(date: String, count: Int) {
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.Bottom) {
            Text(formatDate(date), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text("$count개", color = SecondaryInk, fontSize = 12.sp)
        }
    }

    @Composable
    private fun ScheduleCard(event: ScheduleEvent) {
        GlassCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .055f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.CalendarMonth, null, tint = Ink, modifier = Modifier.size(21.dp)) }
                Column(Modifier.padding(start = 13.dp).weight(1f)) {
                    Text(event.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOf(event.time, event.type).filter { it.isNotBlank() }.joinToString("  ·  ")
                    if (meta.isNotBlank()) Text(meta, color = SecondaryInk, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    if (event.detail.isNotBlank()) Text(event.detail, color = SecondaryInk, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    @Composable
    private fun ChartScreen(state: WidgetState<ChartResponse>) {
        StateContainer(state, "차트를 불러오지 못했어요") { data ->
            var platform by remember(data) { mutableStateOf(data.platforms.firstOrNull()) }
            val rankedSongs = remember(data, platform) {
                data.songs.mapNotNull { song ->
                    platform?.let { p -> song.ranks[p]?.let { song to it } }
                }.sortedBy { it.second.rank }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    SectionIntro(
                        Modifier.padding(horizontal = 18.dp),
                        "CHART", "오늘의 차트", "${rankedSongs.size}곡 · 플랫폼을 선택해서 비교해보세요."
                    )
                }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.platforms.forEach { p ->
                            FilterChip(
                                selected = p == platform, onClick = { platform = p },
                                label = { Text(p, fontSize = 12.sp) },
                                leadingIcon = if (p == platform) { { Icon(Icons.Filled.TrendingUp, null, Modifier.size(15.dp)) } } else null,
                                shape = RoundedCornerShape(18.dp)
                            )
                        }
                    }
                }
                if (rankedSongs.isEmpty()) item { Box(Modifier.padding(horizontal = 18.dp)) { EmptyCard("표시할 순위가 없어요", "다른 플랫폼을 선택해보세요.") } }
                items(rankedSongs) { (song, rank) -> ChartCard(song, rank, Modifier.padding(horizontal = 18.dp)) }
            }
        }
    }

    @Composable
    private fun ChartCard(song: ChartSong, rank: ChartRank, modifier: Modifier = Modifier) {
        val delta = rank.previousRank?.let { it - rank.rank }
        GlassCard(modifier) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(rank.rank.toString().padStart(2, '0'), Modifier.width(42.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (!song.albumImageUrl.isNullOrBlank()) {
                    AsyncImage(model = song.albumImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                } else {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE9E9ED)))
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(song.songName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, color = SecondaryInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
                when {
                    delta == null -> Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryInk)
                    delta > 0 -> RankChange(Icons.Filled.ArrowUpward, "+$delta")
                    delta < 0 -> RankChange(Icons.Filled.ArrowDownward, "$delta")
                    else -> Text("—", color = SecondaryInk, fontSize = 13.sp)
                }
            }
        }
    }

    @Composable private fun RankChange(icon: ImageVector, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = SecondaryInk)
            Spacer(Modifier.width(2.dp))
            Text(text, color = SecondaryInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun SettingsScreen() {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            SectionIntro("SETTINGS", "설정", "설정 화면은 준비 중입니다.")
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Coming soon", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "알림, 데이터 갱신 주기, 표시 옵션 등을 이곳에서 관리할 수 있게 만들 예정입니다.",
                        color = SecondaryInk, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun <T> StateContainer(state: WidgetState<T>, errorTitle: String, content: @Composable (T) -> Unit) {
        when (state) {
            is WidgetState.Loaded -> content(state.data)
            is WidgetState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                    Text("불러오는 중…", color = SecondaryInk, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            is WidgetState.Failed -> Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                EmptyCard(errorTitle, "새로고침을 눌러 다시 시도해보세요.")
            }
        }
    }

    @Composable
    private fun SectionIntro(modifier: Modifier = Modifier, eyebrow: String, title: String, detail: String) {
        Column(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 5.dp)) {
            Text(eyebrow, color = SecondaryInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp, modifier = Modifier.padding(top = 3.dp))
            Text(detail, color = SecondaryInk, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }

    @Composable
    private fun EmptyCard(title: String, detail: String) {
        GlassCard { Column(Modifier.padding(20.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = SecondaryInk, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        } }
    }

    @Composable
    private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
        Surface(
            modifier = modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp), clip = false),
            shape = RoundedCornerShape(22.dp),
            color = Glass,
            border = BorderStroke(1.dp, Hairline),
            content = content
        )
    }

    @Composable
    private fun FloatingGlassBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier.padding(horizontal = 18.dp, vertical = 12.dp).shadow(12.dp, RoundedCornerShape(31.dp)),
            shape = RoundedCornerShape(31.dp),
            color = Color.White.copy(alpha = .82f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .92f))
        ) {
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Tab.values().forEach { NavItem(it, selected, onSelect) }
            }
        }
    }

    @Composable
    private fun NavItem(tab: Tab, selected: Tab, onSelect: (Tab) -> Unit) {
        val active = tab == selected
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(if (active) Color.White.copy(alpha = .96f) else Color.Transparent)
                .clickable { onSelect(tab) }
                .padding(horizontal = 15.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(tab.icon, tab.title, tint = if (active) Ink else SecondaryInk, modifier = Modifier.size(21.dp))
            Text(tab.title, color = if (active) Ink else SecondaryInk, fontSize = 10.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
        }
    }

    private fun formatDate(value: String): String {
        return try {
            val date = LocalDate.parse(value)
            val day = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN)
            "${date.monthValue}월 ${date.dayOfMonth}일 $day"
        } catch (_: DateTimeParseException) { value }
    }
}
