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

// ─── Mock 悬赏数据 ──────────────────────────────

private data class Bounty(
    val id: String, val request: String, val poster: String,
    val reward: Int, val replies: Int, val status: String  // open | resolved
)

private val BOUNTIES = listOf(
    Bounty("1", "求一张表达甲方改需求时内心崩溃的表情包", "崩溃的打工人", 50, 12, "open"),
    Bounty("2", "谁的收藏里有高清版本的熊猫人震惊？求分享", "表情猎人", 30, 8, "open"),
    Bounty("3", "急需一张能表达「表面冷静内心狂喜」的表情包", "心情复杂", 50, 15, "open"),
    Bounty("4", "有没有适合在群里发红包时用的感谢表情包", "红包狂魔", 20, 6, "resolved"),
    Bounty("5", "求一张适合回复老板的「收到」表情包，要卑微一点的", "社畜一枚", 40, 22, "resolved"),
    Bounty("6", "求周杰伦风格的配文表情包，最好是早期专辑截图", "Jay粉", 30, 9, "open"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BountyScreen(onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("💰 表情包悬赏", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // 说明栏
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("💡 找不到想要的表情包？发布悬赏让社区帮你找！采纳后发布者获得积分奖励。",
                modifier = Modifier.padding(14.dp), fontSize = 13.sp)
        }

        // 悬赏列表
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BOUNTIES) { bounty ->
                BountyCard(bounty)
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    // 发布悬赏 FAB
    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Filled.Add, "发布悬赏") }
    }

    // 发布弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("发布悬赏") },
            text = {
                OutlinedTextField(
                    value = dialogText,
                    onValueChange = { dialogText = it },
                    label = { Text("描述你需要什么表情包…") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (dialogText.isNotBlank()) { showDialog = false; dialogText = "" }
                }) { Text("发布（积分 -20）") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BountyCard(bounty: Bounty) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(bounty.request, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bounty.poster, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Chip(Modifier, "积分 ${bounty.reward}", MaterialTheme.colorScheme.tertiaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Chip(Modifier, "${bounty.replies} 回复", MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    if (bounty.status == "resolved") {
                        Chip(Modifier, "已解决", MaterialTheme.colorScheme.errorContainer)
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Chip(modifier: Modifier, text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
