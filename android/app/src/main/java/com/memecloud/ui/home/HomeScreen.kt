package com.memecloud.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

/** PRD 定义的热门标签 */
private val HOT_TAGS = listOf(
    "#搞笑", "#猫", "#打工人", "#熊猫人", "#震惊",
    "#破防", "#周一", "#干饭", "#摸鱼", "#CP"
)

/** 检索方式 Tab */
private enum class SearchMode(val label: String, val icon: @Composable () -> Unit) {
    CAMERA("拍照", { Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp)) }),
    TEXT("文字", { Icon(Icons.Filled.TextFields, null, Modifier.size(18.dp)) }),
    IMAGE("图片", { Icon(Icons.Filled.Image, null, Modifier.size(18.dp)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(SearchMode.CAMERA) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶部栏 ──
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("表情云库", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("🔍", fontSize = 16.sp)
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: 通知 */ }) {
                    Icon(Icons.Filled.Notifications, "通知")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── 文字搜索栏 ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("🔍  搜索表情包…（如「一只崩溃的猫」）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "清除")
                        }
                    } else {
                        IconButton(onClick = { /* TODO: 发起文字检索 */ }) {
                            Icon(Icons.Filled.Search, "搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── 相机预览区（占页面40%） ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "摄像头预览区",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "后续接入 CameraX",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 拍照按钮 ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = { /* TODO: 拍照检索 */ },
                    modifier = Modifier.size(64.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Filled.Camera,
                        contentDescription = "拍照检索",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 检索方式切换 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SearchMode.entries.forEach { mode ->
                    val selected = selectedMode == mode
                    AssistChip(
                        onClick = { selectedMode = mode },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                mode.icon()
                                Spacer(Modifier.width(6.dp))
                                Text(mode.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        leadingIcon = null,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        border = null
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 热门标签 ──
            Text(
                "📌 热门标签",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HOT_TAGS) { tag ->
                    SuggestionChip(
                        onClick = { searchQuery = tag },
                        label = { Text(tag) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
