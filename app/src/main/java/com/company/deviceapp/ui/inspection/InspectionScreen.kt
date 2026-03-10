package com.company.deviceapp.ui.inspection

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraXPreview // 核心防报错：取别名防止与 Compose Preview 冲突
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel // Hilt ViewModel 注入

@Composable
fun InspectionScreen(
    onBack: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel() // 真正挂载 ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 企业级动态请求硬件摄像头权限
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFECEFF1))) {

        // ==========================================
        // 左侧：真实的硬件 CameraX 预览区
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                HardwareCameraPreview(modifier = Modifier.fillMaxSize())
            } else {
                Text("正在请求底层硬件摄像头权限...", color = Color.Gray, fontSize = 28.sp)
            }

            // UI 蒙层覆盖在摄像头画面上
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                when (uiState.currentStep) {
                    InspectionStep.WAITING_FACE_LOGIN -> {
                        Text("请正视摄像头进行人脸捕获", fontSize = 32.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.onFaceRecognized("1", "DEVICE_SN") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) { Text("模拟：人脸识别成功", fontSize = 24.sp) }
                    }
                    InspectionStep.DOING_INSPECTION -> {
                        Text("识别通过。请将双手置于检测区域...", fontSize = 32.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    }
                    InspectionStep.SUBMITTING -> {
                        CircularProgressIndicator(color = Color(0xFF00E676))
                        Text("正在上报晨检记录...", fontSize = 28.sp, color = Color.White)
                    }
                    else -> {}
                }
            }
        }

        // ==========================================
        // 右侧：业务交互区
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(64.dp).background(Color.White, CircleShape)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", modifier = Modifier.size(36.dp), tint = Color(0xFF455A64))
                }
                Spacer(modifier = Modifier.width(32.dp))
                Text("健康监测终端工作台", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF263238))
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (uiState.currentStep == InspectionStep.WAITING_FACE_LOGIN) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("终端处于待机唤醒状态...", fontSize = 36.sp, color = Color(0xFF90A4AE))
                    }
                }
            } else {
                // 已登录，展示用户信息
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        Text("当前人员：${uiState.currentUser?.name ?: "未知"}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                        Spacer(modifier = Modifier.height(24.dp))
                        if (uiState.lastRecord != null) {
                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp)).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("今日最近晨检体温：${uiState.lastRecord!!.temperature}°C | 结果正常", fontSize = 24.sp, color = Color(0xFF1B5E20))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("今日晨检问卷：", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                Spacer(modifier = Modifier.height(16.dp))

                // 这里渲染 viewModel 中获取到的问卷数据
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.questionnaires) { question ->
                        // 此处可以拆分单个问卷组件，暂用文本代替占位
                        Text(text = "${question.questionTitle}", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.submitInspection()
                        Toast.makeText(context, "晨检提交成功", Toast.LENGTH_SHORT).show()
                        viewModel.resetToNextPerson()
                    },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                ) {
                    Text("完成体温与手部检测，提交晨检", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 调用 CameraX 硬件的底层代码
@Composable
fun HardwareCameraPreview(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraXPreview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
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