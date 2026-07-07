package com.memecloud.ui.community

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Mock 话题数据 ──────────────────────────────

private data class ChallengeTopic(
    val id: String, val title: String, val emoji: String,
    val deadline: String, val submissions: Int, val isToday: Boolean
)

private val TOPICS = listOf(
    ChallengeTopic("1", "用表情包表达周一的心情", "😫", "今天 23:59 截止", 47, true),
    ChallengeTopic("2", "假如猫会说话", "🐱", "明天 12:00 截止", 32, true),
    ChallengeTopic("3", "当代大学生的期末状态", "📚", "还剩 3 天", 89, false),
    ChallengeTopic("4", "甲方说「再改一版」时我的反应", "🤬", "还剩 5 天", 156, false),
    ChallengeTopic("5", "打工人的午餐图鉴", "🍱", "已结束", 234, false),
    ChallengeTopic("6", "用一张图表达「我裂开了」", "💥", "已结束", 178, false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicChallengeScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🎯 话题挑战", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // 今日话题横幅
        TOPICS.find { it.isToday }?.let { today ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("🔥 今日挑战", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${today.emoji}  ${today.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(today.deadline, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已有 ${today.submissions} 人参与", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { /* TODO: 去创作 */ }, shape = RoundedCornerShape(20.dp)) {
                            Text("立即参与")
                        }
                        OutlinedButton(onClick = { /* TODO: 查看投稿 */ }, shape = RoundedCornerShape(20.dp)) {
                            Text("查看投稿")
                        }
                    }
                }
            }
        }

        // 往期主题
        Text("往期主题", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TOPICS.filter { !it.isToday }) { topic ->
                TopicCard(topic)
            }
        }
    }
}

@Composable
private fun TopicCard(topic: ChallengeTopic) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(topic.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(topic.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${topic.submissions} 人参与 · ${topic.deadline}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
