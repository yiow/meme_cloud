package com.memecloud.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Mock 用户数据 ──────────────────────────────

private data class FollowUser(
    val id: String, val name: String, val bio: String, val memeCount: Int,
    val followerCount: Int, val isFollowed: Boolean
)

private val FOLLOWING = listOf(
    FollowUser("1", "表情帝", "资深表情包猎人", 89, 256, true),
    FollowUser("2", "猫奴小王", "家里三只猫的铲屎官", 45, 132, true),
    FollowUser("3", "摸鱼大师", "专业摸鱼二十年", 67, 198, true),
    FollowUser("4", "梗图达人", "每天造梗一百个", 156, 420, true),
    FollowUser("5", "斗图冠军", "上届斗图大赛冠军", 234, 512, false),
    FollowUser("6", "社恐星人", "用表情包代替说话", 23, 45, false),
    FollowUser("7", "干饭王", "人间美食记录者", 78, 167, true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(onBack: () -> Unit, initialTab: Int = 0) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }  // 0=关注, 1=粉丝

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("好友关注", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Tab: 关注的 / 粉丝
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("我关注的 (${FOLLOWING.count { it.isFollowed }})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("关注我的 (${FOLLOWING.size - 2})") })
        }

        val list = if (selectedTab == 0) FOLLOWING.filter { it.isFollowed } else FOLLOWING
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { user ->
                FollowCard(user)
            }
        }
    }
}

@Composable
private fun FollowCard(user: FollowUser) {
    var followed by remember { mutableStateOf(user.isFollowed) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像占位
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(user.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
                Row {
                    Text("${user.memeCount} 表情 · ", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${user.followerCount} 粉丝", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 关注/已关注按钮
            if (followed) {
                OutlinedButton(
                    onClick = { followed = false },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("已关注", fontSize = 12.sp) }
            } else {
                Button(
                    onClick = { followed = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("+ 关注", fontSize = 12.sp) }
            }
        }
    }
}
