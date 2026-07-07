package com.memecloud.ui.battle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── Mock 房间数据 ──────────────────────────────

private data class BattleRoom(
    val id: String, val name: String, val emoji: String,
    val online: Int, val tags: List<String>
)

private val ROOMS = listOf(
    BattleRoom("1", "周一摸鱼大队", "🐟", 28, listOf("摸鱼", "打工人")),
    BattleRoom("2", "猫奴集中营", "🐱", 35, listOf("猫", "可爱")),
    BattleRoom("3", "熊猫人总部", "🐼", 42, listOf("熊猫人", "搞笑")),
    BattleRoom("4", "沙雕表情包交流", "🤪", 19, listOf("沙雕", "搞笑")),
    BattleRoom("5", "今日心情：裂开", "💔", 31, listOf("破防", "emo")),
    BattleRoom("6", "干饭人集合", "🍚", 24, listOf("干饭", "美食")),
    BattleRoom("7", "CP粉发疯现场", "💕", 15, listOf("CP", "嗑糖")),
    BattleRoom("8", "深夜emo时间", "🌙", 11, listOf("emo", "深夜")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    onEnterRoom: (String, String) -> Unit = { _, _ -> },
    onGoContest: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("弹幕斗图室", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = onGoContest) {
                    Text("🏆 模仿大赛", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // 说明
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("💬 进入房间后只能用表情包「说话」！弹幕从右到左滚动，纯表情包交流体验~",
                modifier = Modifier.padding(14.dp), fontSize = 13.sp)
        }

        Text("房间大厅", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ROOMS) { room ->
                RoomCard(room, onClick = { onEnterRoom(room.id, room.name) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // 创建房间 FAB
    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { /* TODO: 创建房间 */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Filled.Add, "创建房间") }
    }
}

@Composable
private fun RoomCard(room: BattleRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(room.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Circle, null, Modifier.size(8.dp), tint = Color(0xFF22C55E))
                    Spacer(Modifier.width(4.dp))
                    Text("${room.online}人在线", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    room.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                            Text(tag, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
