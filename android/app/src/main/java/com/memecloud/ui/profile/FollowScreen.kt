package com.memecloud.ui.profile

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memecloud.data.api.MemeApi
import com.memecloud.data.model.FollowUserBrief
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(onBack: () -> Unit, initialTab: Int = 0) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var followingList by remember { mutableStateOf(listOf<FollowUserBrief>()) }
    var followerList by remember { mutableStateOf(listOf<FollowUserBrief>()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    fun load() {
        isLoading = true
        scope.launch {
            try {
                // 使用当前登录用户的 ID 从 token 中获取，这里用 /user/me 拿到自己的 id
                val meResult = api.getMyProfile()
                val myId = meResult.data?.id ?: 0L

                val followingRes = api.getFollowing(myId)
                val followersRes = api.getFollowers(myId)
                if (followingRes.isSuccess && followingRes.data != null) {
                    followingList = followingRes.data.items
                }
                if (followersRes.isSuccess && followersRes.data != null) {
                    followerList = followersRes.data.items
                }
            } catch (e: Exception) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    fun toggleFollow(userId: Long, newState: Boolean) {
        scope.launch {
            try {
                val result = api.toggleFollow(userId)
                if (result.isSuccess) {
                    // 刷新列表
                    load()
                }
            } catch (_: Exception) { }
        }
    }

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

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("我关注的 (${followingList.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("关注我的 (${followerList.size})") })
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val list = if (selectedTab == 0) followingList else followerList
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list) { user ->
                    FollowCard(user, onToggleFollow = { toggleFollow(user.id, user.isFollowed) })
                }
            }
        }
    }
}

@Composable
private fun FollowCard(user: FollowUserBrief, onToggleFollow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.nickname ?: user.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (!user.bio.isNullOrBlank()) {
                    Text(user.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (user.isFollowed) {
                OutlinedButton(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("已关注", fontSize = 12.sp) }
            } else {
                Button(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("+ 关注", fontSize = 12.sp) }
            }
        }
    }
}
