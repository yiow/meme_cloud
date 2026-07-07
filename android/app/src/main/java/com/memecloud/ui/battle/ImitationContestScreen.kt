package com.memecloud.ui.battle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

// ─── 模仿大赛阶段 ──────────────────────────────

private enum class ContestPhase {
    IDLE,        // 待开始
    MATCHING,    // 匹配中
    PLAYING,     // 模仿中（倒计时）
    JUDGING,     // AI 打分中
    RESULT       // 结果显示
}

// ─── Mock 结果 ──────────────────────────────────

private data class ContestResult(val player: String, val score: Int, val isYou: Boolean = false)

@Composable
fun ImitationContestScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf(ContestPhase.IDLE) }
    var countdown by remember { mutableIntStateOf(15) }
    var results by remember { mutableStateOf<List<ContestResult>>(emptyList()) }

    // 模拟匹配
    LaunchedEffect(phase) {
        when (phase) {
            ContestPhase.MATCHING -> {
                delay(2000L + (500..3000).random())
                countdown = 15
                phase = ContestPhase.PLAYING
            }
            ContestPhase.PLAYING -> {
                for (i in 15 downTo 0) {
                    countdown = i
                    delay(1000)
                }
                phase = ContestPhase.JUDGING
            }
            ContestPhase.JUDGING -> {
                delay(2000)
                results = listOf(
                    ContestResult("你", (70..95).random(), true),
                    ContestResult("表情帝", (60..90).random()),
                    ContestResult("摸鱼大师", (55..85).random()),
                    ContestResult("斗图冠军", (65..92).random()),
                ).sortedByDescending { it.score }
                phase = ContestPhase.RESULT
            }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶栏 ──
        Surface(color = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text("🏆 表情包模仿大赛", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // ── 主体内容 ──
        Box(modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center) {
            when (phase) {
                ContestPhase.IDLE -> IdleView(onStart = { phase = ContestPhase.MATCHING })
                ContestPhase.MATCHING -> MatchingView()
                ContestPhase.PLAYING -> PlayingView(countdown)
                ContestPhase.JUDGING -> JudgingView()
                ContestPhase.RESULT -> ResultView(results, onAgain = { phase = ContestPhase.IDLE })
            }
        }
    }
}

@Composable
private fun IdleView(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("表情包模仿大赛", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("系统展示一张目标表情包，你用摄像头模仿它的表情和动作，AI为你打分！",
            textAlign = TextAlign.Center, fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("开始匹配", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MatchingView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        CircularProgressIndicator(Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text("正在匹配对手…", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("预计等待 2-5 秒", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlayingView(countdown: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        // 目标表情包展示
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐼", fontSize = 64.sp)
                    Text("「熊猫人震惊」", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("请模仿这个表情！", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 摄像头占位（后续 CameraX）
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📷 摄像头预览\n（后续接入 CameraX）",
                    textAlign = TextAlign.Center, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        // 倒计时
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape)
                .background(if (countdown <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("$countdown", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text(if (countdown > 0) "倒计时结束后自动拍照" else "拍照中…",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JudgingView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        CircularProgressIndicator(Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text("🤖 AI 打分中…", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("正在分析你的模仿相似度", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultView(results: List<ContestResult>, onAgain: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("🎉", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text("本轮结果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))

        results.forEachIndexed { i, r ->
            val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "  " }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (r.isYou) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(medal, fontSize = 24.sp, modifier = Modifier.width(36.dp))
                    Text(r.player, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${r.score}分", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)) {
            Text("再来一局", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
