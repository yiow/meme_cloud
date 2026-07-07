package com.memecloud.ui.community

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Mock 排行榜数据 ──────────────────────────────

private data class RankItem(val rank: Int, val title: String, val author: String, val likes: Int)
private val TODAY_RANK = List(20) { i ->
    RankItem(i + 1, listOf("周一综合征", "上班摸鱼必备", "甲方又改需求了", "我太难了", "当代大学生实录")[i % 5] + if (i >= 5) " #${i}" else "",
        listOf("表情帝", "梗图达人", "猫奴小王", "摸鱼大师", "斗图冠军")[i % 5], (50..999).random())
}.sortedByDescending { it.likes }
private val WEEK_RANK = TODAY_RANK.shuffled().sortedByDescending { it.likes }
private val TOTAL_RANK = TODAY_RANK.shuffled().sortedByDescending { it.likes }

private val RANK_TABS = listOf("今日最火", "本周最热", "总榜")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val data = when (selectedTab) { 0 -> TODAY_RANK; 1 -> WEEK_RANK; else -> TOTAL_RANK }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🏆 排行榜", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // 三个维度Tab
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            RANK_TABS.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                    text = { Text(title, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) })
            }
        }

        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(data) { item ->
                RankRow(item)
            }
        }
    }
}

@Composable
private fun RankRow(item: RankItem) {
    val medal = when (item.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${item.rank}" }
    val bgColor = when (item.rank) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        3 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
            // 图片占位
            Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.TagFaces, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(item.author, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(4.dp))
            Text("${item.likes}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
