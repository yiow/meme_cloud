package com.memecloud.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.SearchResult
import com.memecloud.data.network.RetrofitClient
import com.memecloud.data.network.toFullUrl
import com.memecloud.ui.camera.CameraPreview
import com.memecloud.ui.camera.rememberCameraState
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

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

    // 摄像头开关
    var isCameraOn by remember { mutableStateOf(false) }

    // CameraX 状态
    val cameraState = rememberCameraState()

    // ── 匹配状态 ──
    var detectedLabel by remember { mutableStateOf("") }
    var matchedImageUrl by remember { mutableStateOf<String?>(null) }
    var matchedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var mode by remember { mutableStateOf("gesture") }
    var matchConfidence by remember { mutableFloatStateOf(0f) }
    var matchError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // ── 搜索状态 ──
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()

    // ── 标签搜索 ──
    fun performSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) { searchResults = emptyList(); return }
        scope.launch {
            isSearching = true
            try {
                val r = RetrofitClient.matchApi.searchLabels(query, mode)
                if (r.isSuccess && r.data != null) {
                    searchResults = r.data
                }
            } catch (_: Exception) {}
            isSearching = false
        }
    }

    // ── 手机拍照匹配 ──
    fun takePhotoAndMatch() {
        scope.launch {
            isLoading = true
            matchError = ""
            try {
                val fileBytes = cameraState.takePhoto()
                if (fileBytes == null) {
                    matchError = "拍照失败，请重试"
                    isLoading = false
                    return@launch
                }
                val requestBody = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "snapshot.jpg", requestBody)
                val modeBody = mode.toRequestBody("text/plain".toMediaTypeOrNull())
                val r = RetrofitClient.matchApi.matchPhoto(part, modeBody)
                if (r.isSuccess && r.data != null) {
                    detectedLabel = r.data.label ?: "无匹配"
                    matchedImageUrl = r.data.image_url
                    matchedImages = r.data.images ?: emptyList()
                    matchConfidence = r.data.confidence.toFloat()
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
                title = {
                    Text(
                        "表情云库",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Filled.Notifications,
                            "通知",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (!isLoading && isCameraOn && hasCameraPermission) 16.dp else 0.dp,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                FloatingActionButton(
                    onClick = { if (isCameraOn && hasCameraPermission) takePhotoAndMatch() },
                    containerColor = if (isLoading) MaterialTheme.colorScheme.secondary
                    else if (!isCameraOn || !hasCameraPermission) MaterialTheme.colorScheme.surfaceVariant
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
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── 摄像头预览区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                if (isCameraOn && hasCameraPermission) {
                    CameraPreview(
                        state = cameraState,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isCameraOn && !hasCameraPermission) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("需要相机权限", color = Color.Gray, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("授予权限")
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Videocam,
                            null,
                            Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("点击开启摄像头", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }

                // 摄像头开关按钮
                IconButton(
                    onClick = {
                        if (!isCameraOn && !hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            isCameraOn = !isCameraOn
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(44.dp)
                        .shadow(
                            elevation = if (isCameraOn && hasCameraPermission) 12.dp else 0.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isCameraOn && hasCameraPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                ) {
                    Icon(
                        imageVector = if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        contentDescription = if (isCameraOn) "关闭摄像头" else "开启摄像头",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == "gesture",
                    onClick = { mode = "gesture" },
                    label = { Text("👋 手势", fontWeight = if (mode == "gesture") FontWeight.SemiBold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = mode == "expr",
                    onClick = { mode = "expr" },
                    label = { Text("😀 表情", fontWeight = if (mode == "expr") FontWeight.SemiBold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.weight(1f))
                if (isLoading) {
                    Text("分析中…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── 搜索栏 ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索表情包…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                Icon(Icons.Filled.Close, "清除")
                            }
                        }
                        IconButton(onClick = { performSearch(searchQuery) }) {
                            Icon(Icons.Filled.Search, "搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )

            // ── 搜索结果 ──
            if (searchResults.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { result ->
                        Column(
                            modifier = Modifier.width(100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${(result.images.firstOrNull() ?: "").toFullUrl()}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = result.label,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            )
                            Text(
                                result.label, fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

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

            // ── 匹配结果 ──
            if (matchedImageUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("匹配结果", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(detectedLabel, style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("置信度 ${(matchConfidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (matchedImages.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Text("全部图片 (${matchedImages.size})", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(matchedImages) { img ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(img.toFullUrl())
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data((matchedImageUrl ?: "").toFullUrl())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 热门标签 ──
            Text(
                "热门标签",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
                letterSpacing = 0.5.sp
            )
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tags = listOf("单手托腮","摊手","捂嘴","手比OK","双手抱头","举手","挠头","大笑","震惊","无语")
                items(tags) { tag ->
                    SuggestionChip(
                        onClick = { performSearch(tag) },
                        label = { Text(tag, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
