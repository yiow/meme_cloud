package com.memecloud.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX 预览 + 拍照封装组件
 *
 * 使用方式：
 * ```
 * val cameraState = rememberCameraState()
 * CameraPreview(state = cameraState, modifier = Modifier.fillMaxSize())
 * // 拍照：
 * val bytes = cameraState.takePhoto()
 * ```
 */

class CameraState internal constructor(
    internal var imageCapture: ImageCapture?,
    private val cameraExecutor: ExecutorService,
) {
    /**
     * 拍照并返回 JPEG 字节数组。
     * 必须在 CameraPreview 已初始化之后调用。
     */
    suspend fun takePhoto(): ByteArray? = suspendCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            Log.w("CameraState", "takePhoto: ImageCapture not ready")
            cont.resume(null)
            return@suspendCoroutine
        }
        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val bytes = image.toJpegByteArray()
                image.close()
                cont.resume(bytes)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraState", "takePhoto error", exception)
                cont.resume(null)
            }
        })
    }

    /**
     * Activity onDestroy 时务必调用，释放相机资源。
     */
    fun release() {
        cameraExecutor.shutdown()
    }
}

/**
 * 在 Composable 作用域内记住一个 CameraState，自动跟随 Lifecycle 释放。
 */
@Composable
fun rememberCameraState(): CameraState {
    val executor = remember { Executors.newSingleThreadExecutor() }
    val state = remember { CameraState(null, executor) }

    DisposableEffect(Unit) {
        onDispose { state.release() }
    }

    return state
}

/**
 * 使用前置摄像头（自拍镜头）作为预览源。
 * 设为 false 则使用后置摄像头。
 */
@Composable
fun CameraPreview(
    state: CameraState,
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 持有 ImageCapture 引用，供 takePhoto() 使用
    val imageCapture = remember { ImageCapture.Builder().build() }

    // 注入到 state 中
    state.imageCapture = imageCapture

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.getSurfaceProvider())

                // 选择摄像头
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // 解绑再绑定
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "bindToLifecycle failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier,
    )
}

// ── 工具函数 ────────────────────────────────────────────

private fun androidx.camera.core.ImageProxy.toJpegByteArray(): ByteArray {
    val bitmap = if (format == android.graphics.ImageFormat.JPEG) {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else {
        // 非 JPEG 格式，通过 YUV → Bitmap 转换
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21, width, height, null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, width, height), 90, out
        )
        BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    // 缩放避免过大 (max 1280px)
    val maxSize = 1280
    val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    } else bitmap

    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
    return stream.toByteArray()
}
