package com.memecloud.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * MJPEG 流解码 composable — 长连接读流 → 后台解码 Bitmap → Image 显示
 * 零频闪，用字节级边界匹配，不依赖 WebView
 */
@Composable
fun MjpegStreamView(streamUrl: String, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(streamUrl) {
        var running = true
        val job = scope.launch(Dispatchers.IO) {
            try {
                val url = URL(streamUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 0
                conn.connect()

                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@launch
                }

                val input = conn.inputStream
                val buf = ByteArray(128 * 1024)  // 128KB
                val jpgBuf = ByteArray(512 * 1024) // 512KB — 足够一帧 JPEG
                var jpgLen = 0

                // 读第一行拿到 boundary: "--frame"
                var boundary = ByteArray(0)
                var i = 0
                while (running) {
                    val b = input.read(); if (b == -1) break
                    if (i >= buf.size) break
                    buf[i++] = b.toByte()
                    if (b == '\n'.code && i >= 2 && buf[i - 2] == '\r'.toByte()) {
                        val line = String(buf, 0, i - 2, Charsets.UTF_8)
                        if (line.startsWith("--")) {
                            boundary = ("\r\n$line\r\n").toByteArray()
                        }
                        break
                    }
                }
                if (boundary.isEmpty()) { conn.disconnect(); return@launch }

                // 逐字节读流，检测 boundary
                val ring = ByteArray(boundary.size)
                var ringPos = 0

                fun ringMatch(): Boolean {
                    for (k in boundary.indices) {
                        if (ring[(ringPos + k) % boundary.size] != boundary[k]) return false
                    }
                    return true
                }

                var inHeaders = true
                var headerBlankLine = 0  // 连续 \r\n 计数

                while (running) {
                    val b = input.read(); if (b == -1) break
                    val byte = b.toByte()

                    if (inHeaders) {
                        // 跳过 HTTP 头直到连续 \r\n\r\n
                        if (byte == '\r'.toByte() || byte == '\n'.toByte()) {
                            headerBlankLine++
                        } else {
                            headerBlankLine = 0
                        }
                        if (headerBlankLine >= 4) {
                            inHeaders = false
                            jpgLen = 0
                        }
                        continue
                    }

                    // 写入 ring buffer
                    ring[ringPos] = byte
                    ringPos = (ringPos + 1) % boundary.size

                    // 写入 jpg buffer
                    if (jpgLen < jpgBuf.size) {
                        jpgBuf[jpgLen++] = byte
                    }

                    // 检查 boundary 匹配
                    if (ringMatch()) {
                        // 去掉尾部 boundary + \r\n
                        val frameLen = jpgLen - boundary.size
                        if (frameLen > 4) {
                            val bmp = BitmapFactory.decodeByteArray(jpgBuf, 0, frameLen)
                            if (bmp != null) {
                                // 不做镜像 — 保证预览=后端=训练数据三者特征一致
                                withContext(Dispatchers.Main) { bitmap = bmp }
                            }
                        }
                        jpgLen = 0
                        inHeaders = true
                        headerBlankLine = 0
                    }
                }
                input.close()
                conn.disconnect()
            } catch (e: Exception) {
                if (e !is CancellationException) e.printStackTrace()
            }
        }
        onDispose {
            running = false
            job.cancel()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "摄像头预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } ?: Text("摄像头连接中…", color = Color.Gray, fontSize = 14.sp)
    }
}


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // ── 匹配状态 ──
    var detectedLabel by remember { mutableStateOf("") }
    var matchedImageUrl by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("gesture") }
    var matchConfidence by remember { mutableFloatStateOf(0f) }
    var matchError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // ── 录制状态 ──
    var isRecording by remember { mutableStateOf(false) }
    var recordLabel by remember { mutableStateOf("") }
    var recordCount by remember { mutableIntStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val streamUrl = "http://10.0.2.2:8001/api/match/camera/stream"

    // ── 拍照匹配 ──
    fun takePhotoAndMatch() {
        scope.launch {
            isLoading = true
            matchError = ""
            try {
                val r = RetrofitClient.matchApi.cameraSnapshot(mode)
                if (r.isSuccess && r.data != null) {
                    detectedLabel = r.data.label ?: "无匹配"
                    matchedImageUrl = r.data.image_url
                    matchConfidence = r.data.confidence.toFloat()
                    if (isRecording && recordLabel.isNotEmpty()) {
                        recordCount++
                    }
                } else {
                    matchError = r.msg
                }
            } catch (e: Exception) {
                matchError = "网络: ${e.message?.take(40) ?: "?"}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表情云库", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Notifications, "通知", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { takePhotoAndMatch() },
                containerColor = if (isLoading) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "拍照匹配",
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── 摄像头预览区（MJPEG 流，零频闪）──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                MjpegStreamView(
                    streamUrl = streamUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // 匹配结果浮动标签
                if (detectedLabel.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color = Color(AndroidColor.parseColor("#CC000000")),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            " $detectedLabel ",
                            color = Color(AndroidColor.parseColor("#7FFFD4")),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── 模式切换 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == "gesture",
                    onClick = { mode = "gesture" },
                    label = { Text("👋 手势") }
                )
                FilterChip(
                    selected = mode == "expr",
                    onClick = { mode = "expr" },
                    label = { Text("😀 表情") }
                )
                Spacer(Modifier.weight(1f))
                if (isLoading) {
                    Text("分析中...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── 搜索栏 ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索表情包…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, "清除")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── 错误信息 ──
            if (matchError.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Color(AndroidColor.parseColor("#33FF0000")),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(matchError, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp))
                }
            }

            // ── 匹配结果卡片 ──
            if (matchedImageUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("http://10.0.2.2:8001${matchedImageUrl}")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp)).background(Color.White)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("匹配结果", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(detectedLabel, style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text("置信度 ${(matchConfidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 录制状态栏 ──
            if (isRecording) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(AndroidColor.parseColor("#CCFF0000")),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔴 录制中: $recordLabel",
                            color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.weight(1f))
                        Text("已录 $recordCount 次",
                            color = Color(AndroidColor.parseColor("#FFCCCC")), fontSize = 11.sp)
                        TextButton(onClick = { isRecording = false; recordLabel = "" }) {
                            Text("停止", color = Color.White)
                        }
                    }
                }
            }

            // ── 热门标签 ──
            Text("热门标签 | 长按录制后拍照", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tags = listOf("单手托腮","摊手","捂嘴","手比OK","双手抱头","举手","挠头","大笑","震惊","无语")
                items(tags) { tag ->
                    SuggestionChip(
                        onClick = { searchQuery = tag },
                        label = { Text(tag, fontSize = 12.sp) },
                        modifier = Modifier.combinedClickable(
                            onClick = { searchQuery = tag },
                            onLongClick = {
                                if (isRecording && recordLabel == tag) {
                                    isRecording = false; recordLabel = ""
                                } else {
                                    isRecording = true; recordLabel = tag; recordCount = 0
                                }
                            }
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
