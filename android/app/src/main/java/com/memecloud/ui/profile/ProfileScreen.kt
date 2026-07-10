package com.memecloud.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memecloud.data.api.MemeApi
import com.memecloud.data.model.UserProfile
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onGoFollow: () -> Unit = {},
    onGoCollections: () -> Unit = {},
    onGoEmojiLibrary: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    LaunchedEffect(Unit) {
        try {
            val result = api.getMyProfile()
            if (result.isSuccess && result.data != null) {
                profile = result.data
            }
        } catch (e: Exception) {
            Toast.makeText(context, "加载用户信息失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("确定退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("我的", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val p = profile
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                // ── 用户信息卡片 ──
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person, contentDescription = null,
                                modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            p?.nickname ?: p?.username ?: "---",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "@${p?.username ?: "---"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            p?.bio ?: "这个人很懒，什么都没写~",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("${p?.postCount ?: 0}", "表情包")
                            StatItem("${p?.points ?: 0}", "积分")
                            StatItem("${p?.followingCount ?: 0}", "关注")
                            StatItem("${p?.followerCount ?: 0}", "粉丝")
                        }
                    }
                }

                // ── 功能入口列表 ──
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        ProfileMenuItem(Icons.Filled.CollectionsBookmark, "我的表情库", "管理上传的表情包", onClick = onGoEmojiLibrary)
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(Icons.Filled.BookmarkBorder, "我的收藏", "已收藏的帖子", onClick = onGoCollections)
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(Icons.Filled.History, "检索历史", "最近 30 天的搜索记录") { }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(Icons.Filled.EmojiEvents, "排行榜", "今日最火 / 本周最热 / 总榜") { }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(
                            Icons.Filled.Groups, "好友关注",
                            "关注 ${p?.followingCount ?: 0} 人，粉丝 ${p?.followerCount ?: 0} 人",
                            onClick = onGoFollow
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
