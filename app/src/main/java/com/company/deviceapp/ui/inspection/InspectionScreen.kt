package com.company.deviceapp.ui.inspection

import android.Manifest
import android.graphics.Rect
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.yannuo.library.faceHelper.CameraUtil
import com.yannuo.library.faceHelper.FaceSDKHandler
import com.yannuo.library.faceHelper.RecognizeCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mcv.facepass.types.FacePassRect

@Composable
fun InspectionScreen(
    onBack: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var faceLoginSession by remember { mutableStateOf(0) }

    var menuExpanded by remember { mutableStateOf(false) }
    var faceLoginActive by remember { mutableStateOf(false) }
    var cardLoginActive by remember { mutableStateOf(false) }

    // 企业级动态请求硬件摄像头权限
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        
        try {
            // 1. 初始化摄像头工具类
            CameraUtil.instance.initCamera(context)
            // 2. 获取所有可用摄像头 ID 列表
            val cameraIdList = CameraUtil.instance.getCameraIdList()
            Log.d("CameraDebug", " 扫描到系统摄像头数量: ${cameraIdList?.size ?: 0}")
            cameraIdList?.forEachIndexed { index, id ->
                Log.d("CameraDebug", " 摄像头索引[$index] -> ID: $id")
            }
        } catch (e: Exception) {
            Log.e("CameraDebug", " 扫描摄像头硬件失败", e)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECEFF1))
    ) {

        // ==========================================
        // 左侧：CameraX 预览区 / FaceSDK 识别区（共用同一块区域）
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF102027)),
            contentAlignment = Alignment.Center
        ) {
            if (faceLoginActive) {
                FaceSdkPreviewArea(
                    sessionId = faceLoginSession,
                    modifier = Modifier.fillMaxSize(),
                    onRecognizedSuccess = { faceToken, faceUrl, faceScore ->
                        Toast.makeText(context, "人脸识别成功", Toast.LENGTH_SHORT).show()
                        faceLoginActive = false
                        viewModel.onFaceRecognized(faceToken, "TEST_DEVICE_001")
                    },
                    onClose = {
                        faceLoginActive = false
                    }
                )
            } else if (cardLoginActive) {
                // 刷卡登录时，显示指定的摄像头4画面
                CardLoginPreviewArea(
                    onClose = { cardLoginActive = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (!hasCameraPermission) {
                    Text(
                        "正在请求底层硬件摄像头权限...",
                        color = Color(0xFFCFD8DC),
                        fontSize = 28.sp
                    )
                } else {
                    IdleCameraOverlay(modifier = Modifier.fillMaxSize())
                }
            }

            if (!faceLoginActive && !cardLoginActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    when (uiState.currentStep) {
                        InspectionStep.DOING_INSPECTION -> {
                            Text(
                                "识别通过。请将双手置于检测区域...",
                                fontSize = 32.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        InspectionStep.SUBMITTING -> {
                            CircularProgressIndicator(color = Color(0xFF00E676))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "正在上报晨检记录...",
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }

        // 右侧：业务交互区
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF455A64)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    Text(
                        "健康监测终端工作台",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF263238)
                    )
                }

                // 右上角登录下拉
                Box {
                    Surface(
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { menuExpanded = true },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0D47A1)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "登录",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "登录",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.6.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "展开",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .width(280.dp)
                            .background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "人脸识别登录",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF263238)
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.resetToNextPerson()
                                faceLoginSession += 1
                                faceLoginActive = true
                                cardLoginActive = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBox,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "刷卡登录",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF263238)
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                faceLoginActive = false
                                cardLoginActive = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 登录激活时，右侧不显示待机提示
            if (uiState.currentStep == InspectionStep.WAITING_FACE_LOGIN && !faceLoginActive && !cardLoginActive) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "终端处于待机唤醒状态...",
                            fontSize = 36.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
            } else if (uiState.currentStep != InspectionStep.WAITING_FACE_LOGIN) {
                // 已登录，展示用户信息
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        Text(
                            "当前人员：${uiState.currentUser?.name ?: "未知"}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.lastRecord != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFFE8F5E9),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "今日最近晨检体温：${uiState.lastRecord!!.temperature}°C | 结果正常",
                                    fontSize = 24.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "今日晨检问卷：",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.questionnaires) { question ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row {
                                    if (question.isRequired) {
                                        Text("* ", color = Color.Red, fontSize = 28.sp)
                                    }
                                    Text(
                                        question.questionTitle,
                                        fontSize = 28.sp,
                                        color = Color(0xFF37474F)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    question.answerList.forEach { option ->
                                        val isSelected = uiState.answers[question.id] == option.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) Color(0xFFE0F2F1)
                                                    else Color(0xFFF5F5F5)
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) Color(0xFF00796B) else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    viewModel.selectAnswer(question.id, option.id)
                                                }
                                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                        ) {
                                            Text(
                                                text = option.optionText,
                                                fontSize = 24.sp,
                                                color = if (isSelected) Color(0xFF00796B) else Color(0xFF78909C),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.submitInspection(
                            onSuccess = {
                                coroutineScope.launch {
                                    Toast.makeText(context, "晨检提交成功", Toast.LENGTH_SHORT).show()
                                    delay(1000)
                                    viewModel.resetToNextPerson()
                                }
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                    enabled = uiState.currentStep == InspectionStep.DOING_INSPECTION
                ) {
                    Text(
                        "完成体温与手部检测，提交晨检",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FaceSdkPreviewArea(
    sessionId: Int,
    modifier: Modifier = Modifier,
    onRecognizedSuccess: (faceToken: String, faceUrl: String, faceScore: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()

    var sdkMessage by remember(sessionId) { mutableStateOf("正在准备人脸识别...") }
    var textureViewRef by remember(sessionId) { mutableStateOf<TextureView?>(null) }
    var surfaceReady by remember(sessionId) { mutableStateOf(false) }
    var sdkStarted by remember(sessionId) { mutableStateOf(false) }

    DisposableEffect(sessionId) {
        onDispose {
            FaceSDKHandler.getInstance().stopFaceDetect()
            FaceSDKHandler.getInstance().closeCamera()
            FaceSDKHandler.getInstance().clearAuthFaceCallback()
        }
    }

    LaunchedEffect(sessionId, textureViewRef, surfaceReady) {
        val textureView = textureViewRef ?: return@LaunchedEffect
        if (!surfaceReady) return@LaunchedEffect
        if (sdkStarted) return@LaunchedEffect
        if (textureView.width <= 0 || textureView.height <= 0) return@LaunchedEffect

        sdkStarted = true
        sdkMessage = "正在初始化人脸SDK..."

        val appContext = context.applicationContext

        FaceSDKHandler.getInstance().initFaceSDK(
            appContext,
            "inspection_group",
            appContext.filesDir.absolutePath
        ) { code, message ->
            activity?.runOnUiThread {
                if (code == 0) {
                    sdkMessage = "SDK初始化成功，正在打开摄像头..."

                    val viewWidth = textureView.width.coerceAtLeast(1)
                    val viewHeight = textureView.height.coerceAtLeast(1)

                    val left = (viewWidth * 0.18f).toInt()
                    val top = (viewHeight * 0.10f).toInt()
                    val right = (viewWidth * 0.99f).toInt().coerceAtMost(viewWidth - 1)
                    val bottom = (viewHeight * 0.90f).toInt()

                    val previewRect = Rect(left, top, right, bottom)

                    val opened = FaceSDKHandler.getInstance().openCamera(
                        previewRect,
                        textureView,
                        object : RecognizeCallback {
                            override fun onPreView(data: ByteArray, width: Int, height: Int) {}
                            override fun onDrawFaceBox(rect: FacePassRect, width: Int, height: Int) {}

                            override fun onRecognized(
                                faceToken: String,
                                faceUrl: String,
                                faceScore: String
                            ) {
                                activity?.runOnUiThread {
                                    sdkMessage = "识别成功，分数: $faceScore"
                                    onRecognizedSuccess(faceToken, faceUrl, faceScore)
                                }
                            }

                            override fun onTips(msg: String) {
                                activity?.runOnUiThread {
                                    sdkMessage = msg
                                }
                            }
                        }
                    )

                    if (opened) {
                        coroutineScope.launch {
                            delay(500)
                            FaceSDKHandler.getInstance().startFaceDetect()
                            sdkMessage = "请面向摄像头进行人脸识别"
                        }
                    } else {
                        sdkMessage = "摄像头打开失败"
                        sdkStarted = false
                    }
                } else {
                    sdkMessage = "SDK初始化失败: $message"
                    sdkStarted = false
                }
            }
        }
    }

    Box(modifier = modifier.background(Color(0xFF102027))) {
        key(sessionId) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: android.graphics.SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                textureViewRef = this@apply
                                surfaceReady = true
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: android.graphics.SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {}

                            override fun onSurfaceTextureDestroyed(
                                surface: android.graphics.SurfaceTexture
                            ): Boolean {
                                surfaceReady = false
                                return true
                            }

                            override fun onSurfaceTextureUpdated(
                                surface: android.graphics.SurfaceTexture
                            ) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!sdkStarted || sdkMessage.contains("初始化") || sdkMessage.contains("准备")) {
            FaceSdkStartupOverlay(
                message = sdkMessage,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.55f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Text(
                    text = sdkMessage,
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(onClick = onClose) {
                    Text("关闭人脸识别", fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
fun FaceSdkStartupOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF102027)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 5.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "正在启动人脸识别",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 22.sp,
                color = Color(0xFFCFD8DC)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "请面向设备并保持稳定",
                fontSize = 20.sp,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}

@Composable
fun HardwareCameraPreview(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraXPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

/**
 * 刷卡登录预览区域，专门显示摄像头 ID 为 4 的画面
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CardLoginPreviewArea(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraXPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // 核心修改：通过 Camera2Interop 强制过滤并选择 ID 为 "4" 的摄像头
                    val cameraSelector = CameraSelector.Builder()
                        .addCameraFilter { cameraInfos ->
                            val filtered = cameraInfos.filter { 
                                Camera2CameraInfo.from(it).cameraId == "4" 
                            }
                            if (filtered.isEmpty()) {
                                Log.w("CameraDebug", "未找到ID为4的摄像头，降级使用后置摄像头")
                                cameraInfos.filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }
                            } else {
                                filtered
                            }
                        }
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    } catch (exc: Exception) {
                        Log.e("CameraDebug", "绑定摄像头4失败", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 覆盖层信息
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(
                    Color.Black.copy(alpha = 0.55f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Text(
                text = "请刷卡进行登录",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("退出刷卡登录", fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun IdleCameraOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "晨检系统待机中",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "点击右上角“登录”开始人脸识别",
                fontSize = 22.sp,
                color = Color(0xFFE3F2FD)
            )
        }
    }
}
