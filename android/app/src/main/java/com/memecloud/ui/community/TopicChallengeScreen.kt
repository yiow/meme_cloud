package com.memecloud.ui.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.MemeApi
import com.memecloud.data.model.*
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicChallengeScreen(onBack: () -> Unit) {
    var topics by remember { mutableStateOf(listOf<TopicBrief>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTopic by remember { mutableStateOf<TopicBrief?>(null) }
    var submissions by remember { mutableStateOf(listOf<TopicSubmissionBrief>()) }
    // false=话题列表, true=投稿表单
    var showPublishForm by remember { mutableStateOf(false) }
    // 投稿表单字段
    var publishImageUrl by remember { mutableStateOf("") }
    var publishCaption by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    fun loadTopics() {
        isLoading = true
        scope.launch {
            try {
                val result = api.getTopics()
                val data = result.data
                if (result.isSuccess && data != null) {
                    topics = data.items
                }
            } catch (e: Exception) {
                Toast.makeText(context, "加载话题失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadSubmissions(topicId: Long) {
        scope.launch {
            try {
                val result = api.getTopicSubmissions(topicId)
                val data = result.data
                if (result.isSuccess && data != null) {
                    submissions = data.items
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadTopics() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    when {
                        showPublishForm -> "我要投稿"
                        selectedTopic != null -> selectedTopic!!.title
                        else -> "话题挑战"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        showPublishForm -> showPublishForm = false
                        selectedTopic != null -> selectedTopic = null
                        else -> onBack()
                    }
                }) { Icon(Icons.Filled.ArrowBack, null) }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // ── 状态 1: 投稿表单 ──
        if (showPublishForm) {
            val topic = selectedTopic!!
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("参与话题", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(topic.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(topic.description ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("图片URL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = publishImageUrl,
                    onValueChange = { publishImageUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入表情包图片URL") },
                    singleLine = true,
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(12.dp)
                )

                if (publishImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = publishImageUrl,
                        contentDescription = "预览",
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }

                Text("配文（可选）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = publishCaption,
                    onValueChange = { publishCaption = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("写下你想说的话...") },
                    maxLines = 3,
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (publishImageUrl.isBlank() || isPublishing) return@Button
                        isPublishing = true
                        scope.launch {
                            try {
                                // 第一步：创建帖子
                                val createReq = PostCreateRequest(
                                    imageUrl = publishImageUrl.trim(),
                                    caption = publishCaption.trim().ifBlank { null }
                                )
                                val createRes = api.createPost(createReq)
                                if (!createRes.isSuccess) {
                                    Toast.makeText(context, createRes.msg, Toast.LENGTH_SHORT).show()
                                    isPublishing = false
                                    return@launch
                                }

                                // 第二步：获取createPost返回的帖子ID
                                val postId = (createRes.data?.get("id") as? Double)?.toLong()
                                if (postId != null) {
                                    val submitRes = api.submitToTopic(topic.id, TopicSubmitRequest(postId))
                                    if (submitRes.isSuccess) {
                                        Toast.makeText(context, "投稿成功！", Toast.LENGTH_SHORT).show()
                                        publishImageUrl = ""
                                        publishCaption = ""
                                        showPublishForm = false
                                        loadSubmissions(topic.id)
                                    } else {
                                        Toast.makeText(context, submitRes.msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "投稿失败：无法获取帖子ID", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "投稿失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isPublishing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = publishImageUrl.isNotBlank() && !isPublishing
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("投稿到「${topic.title}」", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // ── 状态 2: 话题投稿列表 ──
        if (selectedTopic != null) {
            val topic = selectedTopic!!
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("话题", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Text(topic.description ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showPublishForm = true },
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("我来投稿") }
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(submissions) { sub ->
                    SubmissionCard(sub) { submissionId ->
                        scope.launch {
                            try {
                                val r = api.voteSubmission(submissionId)
                                if (r.isSuccess) {
                                    loadSubmissions(topic.id)
                                } else {
                                    Toast.makeText(context, r.msg, Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "投票失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                if (submissions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("暂无投稿，来做第一个！", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            return
        }

        // ── 状态 3: 话题列表 ──
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topics) { topic ->
                    TopicCard(topic, onClick = {
                        selectedTopic = topic
                        loadSubmissions(topic.id)
                    })
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: TopicBrief, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (topic.status == 1) Text("🔥 ", fontSize = 14.sp)
                    Text(topic.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("${topic.submissionCount} 人参与 · ${if (topic.status == 0) "已结束" else "进行中"}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubmissionCard(sub: TopicSubmissionBrief, onVote: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(sub.postImageUrl).crossfade(true).build(),
                contentDescription = sub.postCaption,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sub.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (!sub.postCaption.isNullOrBlank()) {
                        Text(sub.postCaption, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                IconButton(onClick = { onVote(sub.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ThumbUp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text("${sub.voteCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
