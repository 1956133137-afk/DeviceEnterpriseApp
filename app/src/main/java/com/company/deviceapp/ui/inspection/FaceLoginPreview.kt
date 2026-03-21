package com.company.deviceapp.ui.inspection

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yannuo.library.faceHelper.FaceSDKHandler
import com.yannuo.library.faceHelper.RecognizeCallback
import kotlinx.coroutines.delay
import mcv.facepass.types.FacePassRect

@Composable
fun FaceSdkPreviewArea(
    sessionId: Int,
    modifier: Modifier = Modifier,
    onRecognizedSuccess: (faceToken: String, faceUrl: String, faceScore: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var sdkMessage by remember(sessionId) { mutableStateOf("正在启动人脸识别引擎...") }
    var textureViewRef by remember(sessionId) { mutableStateOf<TextureView?>(null) }
    var sdkStarted by remember(sessionId) { mutableStateOf(false) }

    // 用于绘制人脸框的状态
    var faceRect by remember(sessionId) { mutableStateOf<FacePassRect?>(null) }
    var sdkFrameWidth by remember(sessionId) { mutableStateOf(1) }
    var sdkFrameHeight by remember(sessionId) { mutableStateOf(1) }

    // 定义识别区域 (ROI)
    val recognitionROI = remember { Rect(0, 0, 1280, 720) }

    DisposableEffect(sessionId) {
        onDispose {
            FaceSDKHandler.getInstance().stopFaceDetect()
            FaceSDKHandler.getInstance().closeCamera()
            FaceSDKHandler.getInstance().clearAuthFaceCallback()
        }
    }

    LaunchedEffect(sessionId, textureViewRef) {
        val textureView = textureViewRef ?: return@LaunchedEffect
        
        sdkStarted = false
        delay(500)

        val appContext = context.applicationContext
        FaceSDKHandler.getInstance().initFaceSDK(
            appContext,
            "inspection_group",
            appContext.filesDir.absolutePath
        ) { code, message ->
            activity?.runOnUiThread {
                if (code == 0) {
                    sdkMessage = "引擎就绪，正在开启摄像头..."

                    try {
                        val handler = FaceSDKHandler.getInstance()
                        val cameraConfigField = handler.javaClass.getDeclaredField("cameraConfig")
                        cameraConfigField.isAccessible = true
                        val config = cameraConfigField.get(handler)

                        val setRotationMethod = config.javaClass.getMethod("setRGBRotation", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        setRotationMethod.invoke(config, 0, 90)
                        Log.d("FACE_PREVIEW", "已通过反射设置摄像头 ID 4 旋转角度为 90")
                    } catch (e: Exception) {
                        Log.e("FACE_PREVIEW", "无法通过反射设置旋转角度: ${e.message}")
                    }

                    val opened = FaceSDKHandler.getInstance().openCamera(
                        recognitionROI, 
                        textureView,
                        object : RecognizeCallback {
                            override fun onPreView(data: ByteArray, width: Int, height: Int) {}
                            
                            override fun onDrawFaceBox(rect: FacePassRect, width: Int, height: Int) {
                                activity?.runOnUiThread {
                                    faceRect = rect
                                    sdkFrameWidth = width
                                    sdkFrameHeight = height
                                }
                            }
                            
                            override fun onRecognized(token: String, url: String, score: String) {
                                activity?.runOnUiThread { 
                                    onRecognizedSuccess(token, url, score)
                                }
                            }
                            override fun onTips(msg: String) {
                                activity?.runOnUiThread { sdkMessage = msg }
                            }
                        }
                    )

                    if (opened) {
                        sdkStarted = true
                        FaceSDKHandler.getInstance().startFaceDetect()
                    } else {
                        sdkMessage = "摄像头占用冲突"
                    }
                } else {
                    sdkMessage = "初始化失败: $message"
                }
            }
        }
    }

    Box(modifier = modifier.background(Color(0xFF102027))) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { textureViewRef = this@apply }
                        override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scaleX = canvasWidth / sdkFrameWidth
            val scaleY = canvasHeight / sdkFrameHeight

            drawRect(
                color = Color.Yellow.copy(alpha = 0.5f),
                topLeft = Offset(recognitionROI.left.toFloat() * scaleX, recognitionROI.top.toFloat() * scaleY),
                size = Size((recognitionROI.right - recognitionROI.left).toFloat() * scaleX, (recognitionROI.bottom - recognitionROI.top).toFloat() * scaleY),
                style = Stroke(width = 2.dp.toPx())
            )

            faceRect?.let { rect ->
                drawRect(
                    color = Color.Cyan,
                    topLeft = Offset(rect.left.toFloat() * scaleX, rect.top.toFloat() * scaleY),
                    size = Size((rect.right - rect.left).toFloat() * scaleX, (rect.bottom - rect.top).toFloat() * scaleY),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        if (!sdkStarted) {
            FaceSdkStartupOverlay(message = sdkMessage, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Text(text = sdkMessage, fontSize = 28.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onClose) { Text("关闭", fontSize = 22.sp) }
            }
        }
    }
}

@Composable
fun FaceSdkStartupOverlay(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF102027)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color.White, fontSize = 20.sp)
        }
    }
}