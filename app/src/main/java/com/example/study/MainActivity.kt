package com.example.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.study.ui.theme.StudyTheme

// ====================== 数据模型 ======================

enum class DomainType(val label: String, val emoji: String) {
    MEDICAL("医疗领域", "🏥"),
    NON_MEDICAL("非医疗领域", "🌐")
}

data class ArticleMetric(
    val label: String,
    val value: String
)

data class ArticleSection(
    val title: String,
    val content: String
)

data class ArticleItem(
    val id: String,
    val title: String,
    val summary: String,
    val fullContent: String = "",
    val domain: DomainType,
    val tags: List<String>,
    val publishDate: String,
    val source: String,
    val originalUrl: String = "",
    val paperUrl: String = "",
    val readTime: String = "5分钟阅读",
    val imageUrl: String = "",
    val imageCaption: String = "",
    val metrics: List<ArticleMetric> = emptyList(),
    val sections: List<ArticleSection> = emptyList()
)

data class KnowledgeSubTopic(
    val title: String,
    val desc: String,
    val tags: List<String> = emptyList()
)

data class KnowledgeCategory(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val subTopics: List<KnowledgeSubTopic>
)

// ====================== MainActivity ======================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyTheme {
                MainScreen()
            }
        }
    }
}

// ====================== 主界面 (Scaffold + BottomBar + 全屏导航) ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeArticleDetail by remember { mutableStateOf<ArticleItem?>(null) }
    var activeWebViewUrl by remember { mutableStateOf<String?>(null) }
    var activeWebViewTitle by remember { mutableStateOf("") }

    // 优先 1：如果是内置 WebView 路线，打开内置全屏网页阅读器
    if (activeWebViewUrl != null) {
        WeChatWebViewScreen(
            url = activeWebViewUrl!!,
            title = activeWebViewTitle,
            onBack = { activeWebViewUrl = null }
        )
    }
    // 优先 2：如果是选中具体文章，进入独立全文解读页面
    else if (activeArticleDetail != null) {
        ArticleDetailScreen(
            article = activeArticleDetail!!,
            onBack = { activeArticleDetail = null },
            onOpenWebView = { url, title ->
                activeWebViewUrl = url
                activeWebViewTitle = title
            }
        )
    }
    // 基础 Tab 主界面
    else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedTab == 0) "每日 NLP 文章推送" else "NLP 知识库目录",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "每日推送") },
                        label = { Text("每日推送") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "知识目录") },
                        label = { Text("知识目录") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedTab == 0) {
                    DailyPushFeedScreen(
                        onArticleClick = { article -> activeArticleDetail = article }
                    )
                } else {
                    KnowledgeCatalogScreen()
                }
            }
        }
    }
}

// ====================== 1. 每日推送 Feed 页面 ======================

@Composable
fun DailyPushFeedScreen(onArticleClick: (ArticleItem) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sampleArticles by remember { mutableStateOf(loadArticles(context)) }
    var selectedFilter by remember { mutableStateOf("全部") }

    // 自动联网异步拉取 GitHub 云端 GitHub Actions 每日生成的最新文章 JSON 接口
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cloudUrl = "https://raw.githubusercontent.com/abc455/study/main/app/src/main/assets/articles.json"
                val url = java.net.URL(cloudUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val cloudArticles = parseArticlesJsonString(jsonStr)
                    if (cloudArticles.isNotEmpty()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            sampleArticles = cloudArticles
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filterOptions = listOf("全部", "📅 历史归档", "🏥 医疗领域", "🌐 非医疗领域", "MoE", "多模态", "分类任务", "排序任务")

    val filteredArticles = remember(selectedFilter, sampleArticles) {
        sampleArticles.filter { article ->
            when (selectedFilter) {
                "🏥 医疗领域" -> article.domain == DomainType.MEDICAL
                "🌐 非医疗领域" -> article.domain == DomainType.NON_MEDICAL
                "📅 历史归档" -> true
                "全部" -> true
                else -> article.tags.contains(selectedFilter)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 今日统计 Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📅 NLP 每日精选 (已持续累积 ${sampleArticles.size} 篇解读)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "每日自动更新 5 篇医疗 NLP + 5 篇前沿 NLP，历史文章永久沉淀归档，不被覆盖，方便随时回看。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // 筛选 Tag Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文章列表
        if (filteredArticles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无相关分类的文章", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredArticles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }
            }
        }
    }
}

// ====================== 文章卡片 View ======================

@Composable
fun ArticleCard(article: ArticleItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 领域 Tag
                Surface(
                    color = if (article.domain == DomainType.MEDICAL) Color(0xFFE3F2FD) else Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${article.domain.emoji} ${article.domain.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (article.domain == DomainType.MEDICAL) Color(0xFF1565C0) else Color(0xFF7B1FA2),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${article.source} • 📅 ${article.publishDate}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = article.summary,
                fontSize = 13.sp,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 标签组
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                article.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ====================== 2. 文章全文独立详情页面 (无弹窗) ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: ArticleItem,
    onBack: () -> Unit,
    onOpenWebView: (url: String, title: String) -> Unit
) {
    // 监听系统侧滑手势返回：返回上一页列表，不退出 App
    androidx.activity.compose.BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${article.domain.emoji} 文章全文解读",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 元数据行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = if (article.domain == DomainType.MEDICAL) Color(0xFFE3F2FD) else Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${article.domain.emoji} ${article.domain.label}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (article.domain == DomainType.MEDICAL) Color(0xFF1565C0) else Color(0xFF7B1FA2),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${article.source} • ${article.publishDate}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 文章标题
            Text(
                text = article.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 标签栏与发表日期 Banner
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                article.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 明确标注公众号/博客发表日期与来源
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📅 公众号发表日期：${article.publishDate}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⏱️ ${article.readTime}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // 1. 核心指标高亮卡片 Row (例如：诊断准确率 86.5% | 上下文 32k)
            if (article.metrics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    article.metrics.forEach { metric ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = metric.value,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = metric.label,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. 只有原公众号含有真实图片 (imageUrl 不为空) 时才显示图片区域，没有绝不硬加任何卡片/框
            if (article.imageUrl.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.imageCaption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (article.imageCaption.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = article.imageCaption,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. 结构化富文本段落 (分段卡片展示)
            if (article.sections.isNotEmpty()) {
                article.sections.forEach { sec ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = sec.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sec.content,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            } else {
                val contentText = article.fullContent.ifBlank { article.summary }
                Text(
                    text = contentText,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部出处说明
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔗 来源与出处说明",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本文整理自 ${article.source}，涵盖前沿论文核心突破、算法架构与实际任务落地分析。",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val wechatUrl = article.originalUrl.ifBlank { "https://weixin.qq.com" }
            val paperUrl = article.paperUrl.ifBlank { "https://arxiv.org/" }

            // 1. 【核心提升】应用内置网页阅读器直接查看微信公众号原文 (绝不会跳主界面/报参数错)
            Button(
                onClick = {
                    onOpenWebView(wechatUrl, "${article.source} - 微信公众号原文")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "🌐 应用内直接打开【微信公众号】文章原文",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 应用内打开论文官方主页 (Nature / arXiv 概要页，包含期刊信息与 View PDF / HTML 按钮)
            OutlinedButton(
                onClick = {
                    onOpenWebView(paperUrl, "论文官方主页 (Nature / arXiv 概要页)")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "📄 打开论文官方主页 (查看期刊分类 / View PDF)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 调起外部微信客户端/浏览器 (备用)
            TextButton(
                onClick = {
                    openWeChatArticle(context, wechatUrl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💬 尝试调用外部微信客户端打开",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ====================== 3. 原生模型核心架构拓扑图解组件 ======================

@Composable
fun ModelArchitectureFlowchart(caption: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📐 论文与模型核心架构拓扑图解",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "模型架构图",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val cleanCaption = caption.substringAfter("：").ifBlank { caption }
            val steps = cleanCaption.split("➔", "->").map { it.trim() }

            if (steps.size > 1) {
                steps.forEachIndexed { index, step ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⬇",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = caption,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ====================== 应用内内置 WebView 文章阅读器页面 ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatWebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    // 处理侧边滑动手势返回（网页可后退时网页后退，否则退出网页阅读器返回上级文章详情）
    androidx.activity.compose.BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            ).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Text("浏览器打开", fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        
                        setDownloadListener { downloadUrl, _, _, _, _ ->
                            try {
                                val pdfIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(downloadUrl)
                                ).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(pdfIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val targetUrl = request?.url?.toString() ?: ""
                                // 当在论文主页上点击 "View PDF" 时，直接在应用内置阅读器内部加载渲染全文，绝不上弹下载提示或切浏览器
                                if (targetUrl.contains("/pdf/") || targetUrl.endsWith(".pdf", ignoreCase = true)) {
                                    if (targetUrl.contains("arxiv.org")) {
                                        val paperId = targetUrl.substringAfter("/pdf/").substringBefore(".pdf").substringBefore("v")
                                        val ar5ivUrl = "https://ar5iv.labs.arxiv.org/html/$paperId"
                                        view?.loadUrl(ar5ivUrl)
                                        return true
                                    } else {
                                        val docsUrl = "https://docs.google.com/viewer?embedded=true&url=" + targetUrl
                                        view?.loadUrl(docsUrl)
                                        return true
                                    }
                                }
                                return false
                            }

                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedSslError(
                                view: android.webkit.WebView?,
                                handler: android.webkit.SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                handler?.proceed()
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

// ====================== 调起外部微信 App 辅助函数 ======================

fun openWeChatArticle(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        ).apply {
            setPackage("com.tencent.mm")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
        if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val fallbackIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ====================== 3. 知识目录 Screen ======================

@Composable
fun KnowledgeCatalogScreen() {
    val catalog = remember { getMockKnowledgeCatalog() }
    var searchQuery by remember { mutableStateOf("") }
    val expandedCategoryIds = remember { mutableStateListOf<String>() }

    // 搜索过滤 logic
    val filteredCatalog = remember(searchQuery, catalog) {
        if (searchQuery.isBlank()) catalog
        else {
            catalog.mapNotNull { category ->
                val matchingTopics = category.subTopics.filter { sub ->
                    sub.title.contains(searchQuery, ignoreCase = true) ||
                            sub.desc.contains(searchQuery, ignoreCase = true) ||
                            sub.tags.any { it.contains(searchQuery, ignoreCase = true) }
                }
                if (matchingTopics.isNotEmpty() || category.title.contains(searchQuery, ignoreCase = true)) {
                    category.copy(subTopics = matchingTopics.ifEmpty { category.subTopics })
                } else null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            placeholder = { Text("搜索 NLP 概念、任务或模型（如 MoE、分类...）") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCatalog, key = { it.id }) { category ->
                val isExpanded = expandedCategoryIds.contains(category.id) || searchQuery.isNotBlank()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Category Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (expandedCategoryIds.contains(category.id)) {
                                        expandedCategoryIds.remove(category.id)
                                    } else {
                                        expandedCategoryIds.add(category.id)
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = category.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = category.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${category.subTopics.size})",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "展开/收起"
                            )
                        }

                        // SubTopics List
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                category.subTopics.forEach { subTopic ->
                                    SubTopicItemView(subTopic)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubTopicItemView(subTopic: KnowledgeSubTopic) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = subTopic.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subTopic.desc,
                fontSize = 12.sp,
                color = Color.DarkGray,
                lineHeight = 16.sp
            )
            if (subTopic.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    subTopic.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ====================== 假数据生成函数 ======================

fun getMockArticles(): List<ArticleItem> {
    return listOf(
        ArticleItem(
            id = "med_1",
            title = "Med-PaLM 2 临床问答与多标签诊断分类突破",
            summary = "探讨 Google Med-PaLM 2 在复杂临床长文本推理、多标签疾病诊断分类任务上的实验效果与安全对齐策略。",
            fullContent = "📌 一、背景与行业痛点\n电子病历（EHR）和临床主诉文本通常极其冗长，包含大量非标准缩写、实验室检查数值和非结构化叙述。\n\n🔬 二、Med-PaLM 2 核心架构演进\nMed-PaLM 2 是 Google 基于 PaLM 2 架构微调的医疗大语言模型。\n1. 医疗知识对齐（Instruction Tuning）：利用 MultiMedQA 评测集进行指令微调。\n2. 长上下文推理：引入 RoPE 位置编码扩展和 Sparse Attention 稀疏注意力。\n\n🎯 三、在医疗分类与诊断任务中的表现\n在包含 24 种常见临床科室疾病的真人群体诊断测试中，Med-PaLM 2 达到了 86.5% 的准确率。",
            domain = DomainType.MEDICAL,
            tags = listOf("分类任务", "多模态", "医疗大模型"),
            publishDate = "2025-02-24",
            source = "Nature Medicine"
        ),
        ArticleItem(
            id = "nonmed_1",
            title = "DeepSeek-V3 架构解析：细粒度 MoE 专家架构与多头潜在注意力",
            summary = "深度拆解 DeepSeek-V3 核心创新：细粒度专家路由、无辅助损失的负载均衡算法以及大规模推理成本优化。",
            fullContent = "📌 一、DeepSeek-V3 架构革新\nDeepSeek-V3 采用了全新的混合专家架构 (Mixture-of-Experts)，在维持高性能的同时大幅降低了训练与推理吞吐成本。\n\n🔬 二、核心创新点解析\n1. 细粒度专家路由：将原先的大专家拆分为更小粒度的微专家，大幅提升专家表达灵活性。\n2. 无辅助损失负载均衡：引入路由偏置项，实现完美的专家负载均衡。\n3. Multi-Head Latent Attention (MLA): 大幅压缩 KV Cache 显存开销。",
            domain = DomainType.NON_MEDICAL,
            tags = listOf("MoE", "模型架构", "LLM"),
            publishDate = "2025-02-24",
            source = "DeepSeek Technical Report"
        )
    )
}

fun getMockKnowledgeCatalog(): List<KnowledgeCategory> {
    return listOf(
        KnowledgeCategory(
            id = "cat_1",
            title = "NLP 基础与文本预处理",
            iconEmoji = "🔤",
            subTopics = listOf(
                KnowledgeSubTopic("Tokenization (分词技术)", "包含 BPE (Byte-Pair Encoding)、WordPiece 与 SentencePiece 等切词算法原理及对比。", listOf("基础概念", "预处理")),
                KnowledgeSubTopic("词向量与文本表示 (Embedding)", "从 Word2Vec、GloVe 到 Contextual Embedding (BERT)，理解语义空间表示的演进。", listOf("向量表示")),
                KnowledgeSubTopic("Self-Attention & Transformer 机制", "缩放点积注意力、多头注意力 (MHA/GQA/MQA) 及其在序列建模中的核心数学推导。", listOf("Transformer"))
            )
        ),
        KnowledgeCategory(
            id = "cat_2",
            title = "NLP 核心任务 (分类与排序)",
            iconEmoji = "🎯",
            subTopics = listOf(
                KnowledgeSubTopic("文本分类任务 (Classification)", "单标签分类、多标签分类 (Multi-Label)、层次化分类 (HTC) 及极端多标签分类 (XMLC)。", listOf("分类任务", "核心")),
                KnowledgeSubTopic("文本检索与重排序 (Retrieval & Reranking)", "双塔模型 (Bi-Encoder) 粗排与交叉编码器 (Cross-Encoder) 精排，RAG 系统的核心环节。", listOf("排序任务", "RAG")),
                KnowledgeSubTopic("命名实体识别与信息抽取 (NER & IE)", "从 CRF、BiLSTM-CRF 到基于 LLM 提示词的非结构化数据抽取。", listOf("信息抽取"))
            )
        ),
        KnowledgeCategory(
            id = "cat_3",
            title = "MoE 混合专家架构专题",
            iconEmoji = "⚡",
            subTopics = listOf(
                KnowledgeSubTopic("Sparse MoE 基本原理", "路由机制 (Top-k Router)、门控网络 (Gating Network) 以及专家容量上限限制 (Expert Capacity)。", listOf("MoE", "架构")),
                KnowledgeSubTopic("MoE 训练中的负载均衡与辅助损失", "解决路由坍塌与专家闲置问题的 Auxiliary Loss 机制与无损失均衡路由策略。", listOf("MoE", "训练优化")),
                KnowledgeSubTopic("主流 MoE 模型对比 (Mixtral / DeepSeek-V3)", "分析 8x7B、8x22B 以及细粒度路由 MoE 在显存开销和推理吞吐上的优势。", listOf("MoE", "LLM"))
            )
        ),
        KnowledgeCategory(
            id = "cat_4",
            title = "多模态 NLP (Vision-Language)",
            iconEmoji = "🖼️",
            subTopics = listOf(
                KnowledgeSubTopic("图文对齐与对比学习 (CLIP)", "Vision-Language 双塔架构，通过图像-文本对齐实现强大的零样本分类能力。", listOf("多模态", "分类")),
                KnowledgeSubTopic("视觉语言大模型 (VLM Architecture)", "LLaVA、Qwen-VL 等将图像特征投影到文本 token 空间的模态连接层 (Projection Layer)。", listOf("多模态", "VLM"))
            )
        ),
        KnowledgeCategory(
            id = "cat_5",
            title = "医疗 NLP 垂直领域应用",
            iconEmoji = "🏥",
            subTopics = listOf(
                KnowledgeSubTopic("临床病历文本分类与 ICD 编码", "自动化分析电子病历 (EHR)，将临床诊断与手术操作映射到标准 ICD-10/ICD-11 编码。", listOf("医疗NLP", "分类任务")),
                KnowledgeSubTopic("医学文献向量检索与文献重排", "PubMed/MeSH 知识库上的专业术语检索、BioBERT 嵌入与医学 RAG 系统实现。", listOf("医疗NLP", "排序任务")),
                KnowledgeSubTopic("医疗多模态问答 (Med-VQA)", "融合放射科 X 光、CT 影像与临床主诉文本的多模态联合判断与诊断推荐。", listOf("医疗NLP", "多模态"))
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    StudyTheme {
        MainScreen()
    }
}

// ====================== JSON 数据加载（支持实爬/GitHub云端/Asset） ======================

fun parseArticlesJsonString(jsonString: String): List<ArticleItem> {
    try {
        val jsonArray = org.json.JSONArray(jsonString)
        val list = mutableListOf<ArticleItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val domainStr = obj.optString("domain", "NON_MEDICAL")
            val domain = if (domainStr == "MEDICAL") DomainType.MEDICAL else DomainType.NON_MEDICAL
            val tagsArray = obj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(j))
                }
            }

            val metricsArray = obj.optJSONArray("metrics")
            val metrics = mutableListOf<ArticleMetric>()
            if (metricsArray != null) {
                for (m in 0 until metricsArray.length()) {
                    val mObj = metricsArray.getJSONObject(m)
                    metrics.add(ArticleMetric(mObj.optString("label"), mObj.optString("value")))
                }
            }

            val sectionsArray = obj.optJSONArray("sections")
            val sections = mutableListOf<ArticleSection>()
            if (sectionsArray != null) {
                for (s in 0 until sectionsArray.length()) {
                    val sObj = sectionsArray.getJSONObject(s)
                    sections.add(ArticleSection(sObj.optString("title"), sObj.optString("content")))
                }
            }

            list.add(
                ArticleItem(
                    id = obj.optString("id", "id_$i"),
                    title = obj.optString("title", ""),
                    summary = obj.optString("summary", ""),
                    fullContent = obj.optString("fullContent", ""),
                    domain = domain,
                    tags = tags,
                    publishDate = obj.optString("publishDate", "2025-02-24"),
                    source = obj.optString("source", "微信公众号"),
                    originalUrl = obj.optString("originalUrl", ""),
                    paperUrl = obj.optString("paperUrl", ""),
                    readTime = obj.optString("readTime", "5分钟阅读"),
                    imageUrl = obj.optString("imageUrl", ""),
                    imageCaption = obj.optString("imageCaption", ""),
                    metrics = metrics,
                    sections = sections
                )
            )
        }
        return list
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

fun loadArticles(context: android.content.Context): List<ArticleItem> {
    try {
        val jsonString = context.assets.open("articles.json").bufferedReader().use { it.readText() }
        val parsed = parseArticlesJsonString(jsonString)
        if (parsed.isNotEmpty()) return parsed
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return getMockArticles()
}
