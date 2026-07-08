package com.memecloud.ui.battle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ─── Mock 弹幕消息 ──────────────────────────────

private data class BarrageMsg(val id: Long, val emoji: String, val sender: String, val yOffset: Float)

private val EMOJIS = listOf("😂", "🤣", "😭", "💀", "🐱", "🐼", "🤡", "🔥", "💩", "😱", "🥹", "😤",
    "🙄", "😏", "😎", "🫠", "🤯", "🥶", "🤬", "😇", "💀", "👻", "🎃", "😈")

private val SENDERS = listOf("表情帝", "猫奴小王", "摸鱼大师", "斗图冠军", "社恐星人", "干饭王")

@Composable
fun BattleRoomScreen(roomId: String, roomName: String, onBack: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<BarrageMsg>()) }
    var msgId by remember { mutableLongStateOf(0L) }
    val onlineCount = remember { mutableIntStateOf(Random.nextInt(5, 35)) }

    // 模拟弹幕消息到达
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(800, 2500))
            messages = (messages + BarrageMsg(
                id = ++msgId,
                emoji = EMOJIS.random(),
                sender = SENDERS.random(),
                yOffset = Random.nextFloat() * 0.7f
            )).takeLast(50) // 最多保留50条
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶部信息栏 ──
        Surface(color = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(roomName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Circle, null, Modifier.size(8.dp),
                            tint = androidx.compose.ui.graphics.Color(0xFF22C55E))
                        Spacer(Modifier.width(4.dp))
                        Text("${onlineCount.intValue} 人在线", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
                IconButton(onClick = { /* TODO: 分享房间 */ }) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // ── 弹幕区 ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 空状态
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这个房间还没人说话~\n发个表情包暖暖场吧！",
                        textAlign = TextAlign.Center, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 弹幕动画
            messages.forEach { msg ->
                key(msg.id) {
                    AnimatedBarrage(
                        msg = msg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (msg.yOffset * 400).dp)
                    )
                }
            }
        }

        // ── 表情包选择栏 ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ChipButton("我的表情", Icons.Filled.CollectionsBookmark)
                    ChipButton("搜索", Icons.Filled.Search)
                    ChipButton("拍照", Icons.Filled.CameraAlt)
                }
                Spacer(Modifier.height(10.dp))
                // 快捷表情包栏
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EMOJIS.take(12)) { emoji ->
                        Surface(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                            color = MaterialTheme.colorScheme.surface,
                            onClick = {
                                messages = (messages + BarrageMsg(
                                    id = ++msgId, emoji = emoji,
                                    sender = "我", yOffset = Random.nextFloat() * 0.7f
                                )).takeLast(50)
                            }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBarrage(msg: BarrageMsg, modifier: Modifier) {
    val offsetX = remember { Animatable(1000f) }

    LaunchedEffect(msg.id) {
        offsetX.snapTo(1000f)
        offsetX.animateTo(
            targetValue = -2000f,
            animationSpec = tween(durationMillis = 12000, easing = LinearEasing)
        )
    }

    Row(
        modifier = modifier
            .offset(x = offsetX.value.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (msg.sender == "我") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(msg.emoji, fontSize = 28.sp)
        Spacer(Modifier.width(6.dp))
        Text(msg.sender, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChipButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AssistChip(
        onClick = { },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 12.sp)
            }
        }
    )
}
