package com.memecloud.ui.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.MemeApi
import com.memecloud.data.model.RankPost
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch

private val RANK_TABS = listOf("今日最火" to "today", "本周最热" to "week", "总榜" to "all")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(onBack: () -> Unit, onGoDetail: (Long) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(listOf<RankPost>()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    fun load(period: String) {
        isLoading = true
        scope.launch {
            try {
                val result = api.getRanking(period = period)
                val data = result.data
                if (result.isSuccess && data != null) {
                    items = data.items
                }
            } catch (e: Exception) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) { load(RANK_TABS[selectedTab].second) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("排行榜", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
        ) {
            RANK_TABS.forEachIndexed { i, (label, _) ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                    text = { Text(label, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) })
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(items) { item ->
                    RankRow(item, onClick = { onGoDetail(item.id) })
                }
            }
        }
    }
}

@Composable
private fun RankRow(item: RankPost, onClick: () -> Unit) {
    val medal = when (item.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${item.rank}" }
    val bgColor = when (item.rank) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        3 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(medal, fontSize = if (item.rank <= 3) 28.sp else 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.caption,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.caption ?: "无描述", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(item.authorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(4.dp))
            Text("${item.likeCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
