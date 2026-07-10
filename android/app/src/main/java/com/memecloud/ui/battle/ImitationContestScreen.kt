package com.memecloud.ui.battle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.PlayerResult
import com.memecloud.data.api.RandomTarget
import com.memecloud.data.network.RetrofitClient
import com.memecloud.data.network.toFullUrl
import com.memecloud.data.network.toWsUrl
import com.memecloud.ui.camera.CameraPreview
import com.memecloud.ui.camera.rememberCameraState
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─── 模仿大赛阶段 ──────────────────────────────

private enum class ContestPhase {
    IDLE, MATCHING, PLAYING, JUDGING, RESULT
}

// ─── WebSocket 消息 ──────────────────────────────

private data class WsMessage(val type: String, val data: JSONObject? = null)

@Composable
fun ImitationContestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(ContestPhase.IDLE) }
    var countdown by remember { mutableIntStateOf(5) }
    var errorMsg by remember { mutableStateOf("") }

    // ── 摄像头权限 ──
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // CameraX 状态
    val cameraState = rememberCameraState()

    // 目标表情包
    var target by remember { mutableStateOf<RandomTarget?>(null) }

    // 结果
    var myScore by remember { mutableIntStateOf(0) }
    var opponentScore by remember { mutableIntStateOf(0) }
    var opponentName by remember { mutableStateOf("") }

    // WebSocket
    var ws by remember { mutableStateOf<WebSocket?>(null) }
    var myUserId by remember { mutableIntStateOf(0) }
    var matchId by remember { mutableIntStateOf(0) }
    var opponentReady by remember { mutableStateOf(false) }

    // ── WebSocket 连接 ──
    fun connectWebSocket(userId: Int) {
        // 关闭旧连接（避免重复连接导致 403）
        ws?.close(1000, "reconnect")
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder()
            .url("/api/game/ws/$userId".toWsUrl())
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                when (json.optString("type")) {
                    "waiting" -> { /* 等待中，MATCHING 阶段已显示 */ }
                    "match_found" -> {
                        val t = json.getJSONObject("target")
                        val newTarget = RandomTarget(
                            emoji_id = t.getString("emoji_id"),
                            label = t.getString("label"),
                            image_url = t.getString("image_url")
                        )
                        matchId = json.getInt("match_id")
                        opponentName = json.optString("opponent", "对手")
                        countdown = json.optInt("countdown", 5)
                        target = newTarget
                        phase = ContestPhase.PLAYING
                    }
                    "opponent_ready" -> {
                        opponentReady = true
                    }
                    "opponent_left" -> {
                        errorMsg = "对手已离开"
                        phase = ContestPhase.IDLE
                    }
                    "result" -> {
                        myScore = json.optInt("my_score", 0)
                        opponentScore = json.optInt("opponent_score", 0)
                        opponentName = json.optString("opponent_name", opponentName)
                        phase = ContestPhase.RESULT
                    }
                    "error" -> {
                        errorMsg = json.optString("msg", "未知错误")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                errorMsg = "连接失败: ${t.message?.take(30)}"
                phase = ContestPhase.IDLE
            }
        })
    }

    // 清理 WebSocket
    DisposableEffect(Unit) {
        onDispose { ws?.close(1000, "") }
    }

    // ── 阶段驱动 ──
    LaunchedEffect(phase) {
        when (phase) {
            ContestPhase.PLAYING -> {
                try {
                    for (i in countdown downTo 0) {
                        countdown = i
                        if (i > 0) delay(1000)
                    }
                    phase = ContestPhase.JUDGING
                } catch (_: CancellationException) {
                    // phase 变化导致 LaunchedEffect 重启，无需处理
                }
            }

            ContestPhase.JUDGING -> {
                val t = target ?: run {
                    errorMsg = "目标数据丢失"
                    phase = ContestPhase.IDLE
                    return@LaunchedEffect
                }
                try {
                    // 手机拍照上传
                    val fileBytes = cameraState.takePhoto()
                    if (fileBytes == null) {
                        errorMsg = "拍照失败，请重试"
                        phase = ContestPhase.IDLE
                        return@LaunchedEffect
                    }
                    val requestBody = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", "contest.jpg", requestBody)
                    val targetLabelBody = t.label.toRequestBody("text/plain".toMediaTypeOrNull())
                    val targetEmojiIdBody = t.emoji_id.toRequestBody("text/plain".toMediaTypeOrNull())
                    val targetImageBody = t.image_url.toRequestBody("text/plain".toMediaTypeOrNull())
                    val userIdBody = myUserId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val matchIdBody = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    val r = RetrofitClient.gameApi.submitPhoto(
                        part, targetLabelBody, targetEmojiIdBody, targetImageBody,
                        userIdBody, matchIdBody
                    )
                    if (r.isSuccess && r.data != null) {
                        myScore = r.data.score
                        if (matchId == 0) {
                            opponentScore = 0
                            opponentName = "AI对手"
                            phase = ContestPhase.RESULT
                        }
                        if (matchId > 0) {
                            // 等待 WS 推送结果（最多 10s）；收到 result 会切 phase 取消此 delay
                            delay(10_000)
                            if (phase == ContestPhase.JUDGING) {
                                errorMsg = "等待对手超时"
                                phase = ContestPhase.RESULT
                            }
                        }
                    } else {
                        errorMsg = r.msg
                        phase = ContestPhase.IDLE
                    }
                } catch (_: CancellationException) {
                    // WS 已推送 result，phase 被改为 RESULT，此协程正常取消
                } catch (e: Exception) {
                    errorMsg = "提交失败: ${e.message?.take(30) ?: "?"}"
                    phase = ContestPhase.IDLE
                }
            }

            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶栏 ──
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ws?.close(1000, "")
                    onBack()
                }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("🏆 模仿大赛", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                if (phase == ContestPhase.PLAYING || phase == ContestPhase.JUDGING) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0x88FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(" vs $opponentName",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 错误提示 ──
        if (errorMsg.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color(0x33FF0000),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)) {
                    Text(errorMsg, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        errorMsg = ""
                        ws?.close(1000, "")
                        target = null
                        matchId = 0
                        opponentReady = false
                    }) { Text("关闭", fontSize = 12.sp) }
                }
            }
        }

        // ── 主体 ──
        Box(modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center) {
            when (phase) {
                ContestPhase.IDLE -> IdleView(onStart = {
                    // 检查权限
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                        return@IdleView
                    }
                    errorMsg = ""
                    myUserId = (100..999).random()
                    connectWebSocket(myUserId)
                    // 发送加入队列
                    ws?.send("{\"type\":\"join_queue\"}")
                    phase = ContestPhase.MATCHING
                })
                ContestPhase.MATCHING -> MatchingView(
                    onCancel = {
                        ws?.send("{\"type\":\"leave_queue\"}")
                        ws?.close(1000, "")
                        phase = ContestPhase.IDLE
                    }
                )
                ContestPhase.PLAYING -> PlayingView(
                    target = target,
                    countdown = countdown,
                    cameraState = cameraState,
                    hasCameraPermission = hasCameraPermission,
                    permissionLauncher = permissionLauncher,
                    opponentName = opponentName,
                    opponentReady = opponentReady,
                )
                ContestPhase.JUDGING -> JudgingView(opponentName = opponentName)
                ContestPhase.RESULT -> ResultView(
                    myScore = myScore,
                    opponentScore = opponentScore,
                    opponentName = opponentName,
                    targetLabel = target?.label ?: "",
                    onAgain = {
                        errorMsg = ""
                        target = null
                        myScore = 0
                        opponentScore = 0
                        opponentName = ""
                        matchId = 0
                        opponentReady = false
                        ws?.close(1000, "")
                        phase = ContestPhase.IDLE
                    }
                )
            }
        }
    }
}

// ── IDLE ──

@Composable
private fun IdleView(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text("表情包模仿大赛", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("实时 2人对战 · AI 智能打分", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎮 玩法", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("1. 系统匹配一名真人对手", fontSize = 13.sp)
                Text("2. 展示目标表情包，5 秒模仿时间", fontSize = 13.sp)
                Text("3. 双方对着摄像头模仿", fontSize = 13.sp)
                Text("4. AI 打分，高分者获胜！", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("开始匹配", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── MATCHING ──

@Composable
private fun MatchingView(onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        CircularProgressIndicator(Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text("正在寻找对手…", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("实时匹配中，请稍候", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text("取消匹配") }
    }
}

// ── PLAYING: 双人分屏 + 目标 + 倒计时 ──

@Composable
private fun PlayingView(
    target: RandomTarget?,
    countdown: Int,
    cameraState: com.memecloud.ui.camera.CameraState,
    hasCameraPermission: Boolean,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    opponentName: String,
    opponentReady: Boolean,
) {
    if (target == null) return

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 目标表情包 + 倒计时 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 目标
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(target.image_url.toFullUrl())
                            .crossfade(true)
                            .build(),
                        contentDescription = target.label,
                        modifier = Modifier.size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("🎯 模仿目标", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text("「${target.label}」", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // 倒计时
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(if (countdown <= 2) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("${countdown}s", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── 双人摄像头画面 ──
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 你的画面
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                ) {
                    Text("👤 你", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        CameraPreview(
                            state = cameraState,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("需要相机权限", color = Color.White, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("授予权限", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 对手画面（对手在另一台设备上，显示占位）
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (opponentReady) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                ) {
                    Text(
                        if (opponentReady) "✅ $opponentName" else "⏳ $opponentName",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .border(2.dp,
                            if (opponentReady) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (opponentReady) "对手已就绪" else "等待对手...",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(if (countdown > 0) "请对着摄像头模仿目标表情包！"
             else "📸 正在拍照评分…",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── JUDGING ──

@Composable
private fun JudgingView(opponentName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)) {
        CircularProgressIndicator(Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text("🤖 AI 打分中…", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("等待 $opponentName 的结果", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── RESULT ──

@Composable
private fun ResultView(
    myScore: Int,
    opponentScore: Int,
    opponentName: String,
    targetLabel: String,
    onAgain: () -> Unit
) {
    val isWin = myScore > opponentScore
    val isDraw = myScore == opponentScore

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(if (isWin) "🎉" else if (isDraw) "🤝" else "💪", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isDraw) "平局！" else if (isWin) "你赢了！" else "$opponentName 获胜",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
        )
        Text("目标: 「$targetLabel」", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        // 双方对比
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 你
            Card(
                modifier = Modifier.weight(1f).padding(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWin) Color(0x334CAF50) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (isWin) "👑" else "👤", fontSize = 36.sp)
                    Text("你", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("$myScore", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                        color = if (isWin) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                    Text("分", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // VS
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("VS", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 对手
            Card(
                modifier = Modifier.weight(1f).padding(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isWin && !isDraw) Color(0x334CAF50) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (!isWin && !isDraw) "👑" else "👤", fontSize = 36.sp)
                    Text(opponentName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("$opponentScore", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                        color = if (!isWin && !isDraw) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                    Text("分", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("再来一局", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
