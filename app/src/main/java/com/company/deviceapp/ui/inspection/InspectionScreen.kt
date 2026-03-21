package com.company.deviceapp.ui.inspection

import android.Manifest
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.yannuo.library.interfaces.QrCodeListener
import com.yannuo.library.interfaces.SerialPortListener
import com.yannuo.library.qrCodeHelper.QrCodeHelper
import com.yannuo.library.serialPortHelper.SerialPortHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

@Composable
fun InspectionScreen(
    onBack: () -> Unit,
    viewModel: InspectionViewModel? = null 
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val actualViewModel: InspectionViewModel? = if (isPreview) null else {
        viewModel ?: hiltViewModel()
    }

    val viewModelRef = remember(actualViewModel) { WeakReference(actualViewModel) }
    
    val uiState by if (actualViewModel != null) {
        actualViewModel.uiState.collectAsState()
    } else {
        remember { mutableStateOf(InspectionUiState(currentStep = InspectionStep.DOING_INSPECTION)) }
    }

    var showQuestionnaireDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(3000)
            actualViewModel?.clearError()
        }
    }

    if (uiState.currentStep == InspectionStep.WAITING_FACE_LOGIN && !isPreview) {
        DisposableEffect(Unit) {
            val qrHelper = QrCodeHelper.getInstance()
            val serialHelper = SerialPortHelper()
            serialHelper.setSerialPort(1)

            val handlerThread = android.os.HandlerThread("HardwareThread").apply { start() }
            val hardwareDispatcher = android.os.Handler(handlerThread.looper).asCoroutineDispatcher()

            val job = coroutineScope.launch(hardwareDispatcher) {
                try {
                    Runtime.getRuntime().exec("su -c chmod 666 /dev/ttyS0")
                    Runtime.getRuntime().exec("su -c chmod 666 /dev/ttyS4")

                    qrHelper.setUSBorCOM("/dev/ttyS0", 4800, true)
                    
                    val appContext = context.applicationContext
                    
                    qrHelper.open(appContext, object : QrCodeListener {
                        override fun onQrCodeSuccess() { Log.d("Hardware", "扫码枪打开成功") }
                        override fun onQrCodeReceived(scanResult: String) {
                            viewModelRef.get()?.let { vm ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    vm.onCardOrQrRecognized(scanResult)
                                }
                            }
                        }
                        override fun onQrCodeFailure(errMsg: String) {}
                        override fun onQrCodeException(excMsg: String) {}
                    })

                    serialHelper.openSerialPort("/dev/ttyS4", 9600, object : SerialPortListener {
                        override fun onSerialPortSuccess() {
                            serialHelper.startSerialPort()
                            Log.d("Hardware", "读卡器就绪 (/dev/ttyS4)")
                        }
                        override fun onSerialPortReceived(dataStr: String, data: ByteArray, size: Int) {
                            viewModelRef.get()?.let { vm ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    vm.onCardOrQrRecognized(dataStr)
                                }
                            }
                        }
                        override fun onSerialPortException(msg: String) { Log.e("Hardware", "读卡异常: $msg") }
                        override fun onSerialPortFailure(msg: String) { Log.e("Hardware", "读卡失败: $msg") }
                    })
                } catch (e: Exception) {
                    Log.e("Hardware", "初始化异常: ${e.message}")
                }
            }

            onDispose {
                job.cancel()
                serialHelper.releaseSerialPort()
                qrHelper.close()
                handlerThread.quitSafely()
                Log.d("Hardware", "页面销毁，已释放所有硬件资源")
            }
        }
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var faceLoginActive by remember { mutableStateOf(false) }
    var faceLoginSession by remember { mutableStateOf(0) }
    var cardLoginActive by remember { mutableStateOf(false) }
    
    var handCaptureActive by remember { mutableStateOf(false) }
    var isBackHandCapture by remember { mutableStateOf(false) }
    var faceCaptureActive by remember { mutableStateOf(false) }
    
    var inspectionStatusMsg by remember { mutableStateOf("正在启动检测...") }

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
        ) {
            Box(
                modifier = Modifier
                    .weight(0.75f) 
                    .fillMaxHeight()
                    .padding(24.dp),
                contentAlignment = Alignment.Center 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.9f) 
                        .clip(RoundedCornerShape(32.dp))
                        .shadow(16.dp, RoundedCornerShape(32.dp))
                        .background(Color(0xFF102027))
                        .border(1.5.dp, Color(0xFF37474F), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPreview) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, null, tint = Color.DarkGray, modifier = Modifier.size(80.dp))
                            Text("摄像头区域预览", color = Color.Gray, fontSize = 24.sp)
                        }
                    } else {
                        when {
                            faceCaptureActive -> {
                                var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CameraId4Preview(
                                        cameraId = "2",
                                        modifier = Modifier.fillMaxSize(),
                                        onFrameCaptured = { currentBitmap = it }
                                    )
                                    
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            onClick = {
                                                currentBitmap?.let { bitmap ->
                                                    val fileName = "face_manual_${System.currentTimeMillis()}.jpg"
                                                    val file = File(context.cacheDir, fileName)
                                                    try {
                                                        FileOutputStream(file).use { out ->
                                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                        }
                                                        actualViewModel?.onFaceImgCaptured(file.absolutePath)
                                                        mainHandler.post {
                                                            Toast.makeText(context, "人脸重拍成功", Toast.LENGTH_SHORT).show()
                                                        }
                                                        faceCaptureActive = false
                                                    } catch (e: Exception) {
                                                        mainHandler.post {
                                                            Toast.makeText(context, "保存人脸失败", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.height(80.dp).width(220.dp),
                                            shape = RoundedCornerShape(40.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                                        ) {
                                            Text("点击拍照", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedButton(
                                            onClick = { faceCaptureActive = false },
                                            modifier = Modifier.height(56.dp).width(140.dp),
                                            border = BorderStroke(2.dp, Color.White),
                                            shape = RoundedCornerShape(28.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Text("取消", fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                            
                            handCaptureActive -> {
                                CameraWithRoboflowPreview(
                                    cameraId = "4",
                                    isBackOfHand = isBackHandCapture,
                                    modifier = Modifier.fillMaxSize(),
                                    onResult = { isQualified, msg, bitmap ->
                                        inspectionStatusMsg = msg
                                        if (isQualified) {
                                            val fileName = "hand_${System.currentTimeMillis()}.jpg"
                                            val file = File(context.cacheDir, fileName)
                                            try {
                                                FileOutputStream(file).use { out ->
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                }
                                                actualViewModel?.onHandRecognized(file.absolutePath, isBackHandCapture)
                                                mainHandler.post {
                                                    Toast.makeText(context, "手部拍照成功", Toast.LENGTH_SHORT).show()
                                                }
                                                handCaptureActive = false
                                            } catch (e: Exception) {
                                                mainHandler.post {
                                                    Toast.makeText(context, "保存手部失败", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            faceLoginActive -> {
                                FaceSdkPreviewArea(
                                    sessionId = faceLoginSession,
                                    modifier = Modifier.fillMaxSize(),
                                    onRecognizedSuccess = { faceToken, faceUrl, _ ->
                                        val copiedFacePath = copyRecognizedFaceImage(context, faceUrl)
                                        Toast.makeText(context, "识别成功", Toast.LENGTH_SHORT).show()
                                        faceLoginActive = false
                                        actualViewModel?.onFaceRecognized(faceToken, copiedFacePath ?: faceUrl, "TEST_DEVICE_001")
                                    },
                                    onClose = { faceLoginActive = false }
                                )
                            }

                            cardLoginActive -> {
                                CardLoginPreviewArea(modifier = Modifier.fillMaxSize(), onClose = { cardLoginActive = false })
                            }

                            else -> {
                                if (!hasCameraPermission) {
                                    Text("等待相机权限...", color = Color.Gray, fontSize = 24.sp)
                                } else {
                                    IdleCameraOverlay(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    if (handCaptureActive || faceCaptureActive) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(20.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = inspectionStatusMsg,
                                color = Color(0xFFFFEB3B),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }

                    if (!faceLoginActive && !cardLoginActive && !handCaptureActive && !faceCaptureActive) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(24.dp)
                        ) {
                            when (uiState.currentStep) {
                                InspectionStep.DOING_INSPECTION -> {
                                    Text("识别成功，请继续后续操作", fontSize = 32.sp, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                                }
                                InspectionStep.SUBMITTING -> {
                                    CircularProgressIndicator(color = Color(0xFF69F0AE), strokeWidth = 4.dp)
                                    Text("数据上报中...", fontSize = 28.sp, color = Color.White)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (isPressed) 0.92f else 1f)
                        
                        Surface(
                            onClick = { onBack() },
                            modifier = Modifier
                                .size(84.dp) 
                                .scale(scale)
                                .shadow(if (isPressed) 2.dp else 8.dp, CircleShape),
                            shape = CircleShape,
                            color = Color.White,
                            interactionSource = interactionSource
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowBack, 
                                    "返回", 
                                    modifier = Modifier.size(42.dp), 
                                    tint = Color(0xFF2C3E50)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("健康监测工作台", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color(0xFF2C3E50))
                    }

                    if (uiState.currentStep == InspectionStep.WAITING_FACE_LOGIN) {
                        Box {
                            Surface(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        cardLoginActive = false
                                        handCaptureActive = false
                                        faceCaptureActive = false
                                        actualViewModel?.resetToNextPerson()
                                        faceLoginSession++
                                        faceLoginActive = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1565C0)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("人脸识别登录", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.currentStep == InspectionStep.WAITING_FACE_LOGIN && !faceLoginActive && !cardLoginActive && !handCaptureActive && !faceCaptureActive) {
                        Box(modifier = Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFBDC3C7), modifier = Modifier.size(120.dp))
                                Text("终端待机中", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF95A5A6))
                                Text("请先刷卡或者扫码登录", fontSize = 44.sp, color = Color(0xFFFF7043))
                            }
                        }
                    } else if (uiState.currentStep != InspectionStep.WAITING_FACE_LOGIN) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.98f)
                                .wrapContentHeight()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                 val isPhotosComplete = !uiState.recognizedFaceImgPath.isNullOrBlank() && 
                                                      !uiState.handImg1Path.isNullOrBlank() && 
                                                      !uiState.handImg2Path.isNullOrBlank()

                                 InspectionRecordInfoCard(
                                    faceImgPath = uiState.recognizedFaceImgPath,
                                    handImg1Path = uiState.handImg1Path,
                                    handImg2Path = uiState.handImg2Path,
                                    memberUserId = uiState.currentUser?.personnelId ?: "",
                                    inspectionTime = uiState.recognizedInspectionTime,
                                    temperature = uiState.lastRecord?.temperature ?: "36.5",
                                    identifyType = uiState.recognizedIdentifyType,
                                    openDoor = uiState.openDoorStatus, 
                                    handType = uiState.lastRecord?.handType ?: "1",
                                    healthCertificate = uiState.lastRecord?.healthCertificate ?: "1",
                                    tempType = uiState.lastRecord?.tempType ?: "1",
                                    status = uiState.lastRecord?.status ?: "1",
                                    username = uiState.currentUser?.name ?: "",
                                    inspectionDesc = uiState.lastRecord?.inspectionDesc ?: "无",
                                    onDeleteFaceImg = { actualViewModel?.clearFaceImg() },
                                    onRetakeFaceImg = { faceCaptureActive = true },
                                    onDeleteHandImg1 = { actualViewModel?.clearHandImg1() },
                                    onRetakeHandImg1 = { 
                                        actualViewModel?.clearHandImg1() 
                                        isBackHandCapture = false
                                        inspectionStatusMsg = "请将手掌对准摄像头"
                                        handCaptureActive = true 
                                    },
                                    onDeleteHandImg2 = { actualViewModel?.clearHandImg2() },
                                    onRetakeHandImg2 = { 
                                        actualViewModel?.clearHandImg2() 
                                        isBackHandCapture = true
                                        inspectionStatusMsg = "请将手背对准摄像头"
                                        handCaptureActive = true 
                                    },
                                    onOpenDoorStatusChange = { actualViewModel?.setOpenDoorStatus(it) },
                                    isPhotosComplete = isPhotosComplete,
                                    canSubmit = uiState.currentStep == InspectionStep.DOING_INSPECTION,
                                    onConfirmSubmit = {
                                        if (uiState.recognizedFaceImgPath.isNullOrBlank()) {
                                            Toast.makeText(context, "请先完成人脸拍照", Toast.LENGTH_SHORT).show()
                                            return@InspectionRecordInfoCard
                                        }
                                        if (uiState.handImg1Path.isNullOrBlank()) {
                                            Toast.makeText(context, "请完成手掌拍照检测", Toast.LENGTH_SHORT).show()
                                            return@InspectionRecordInfoCard
                                        }
                                        if (uiState.handImg2Path.isNullOrBlank()) {
                                            Toast.makeText(context, "请完成手背拍照检测", Toast.LENGTH_SHORT).show()
                                            return@InspectionRecordInfoCard
                                        }
                                        actualViewModel?.submitInspection(
                                            onSuccess = { 
                                                coroutineScope.launch { 
                                                    Toast.makeText(context, "提交成功", Toast.LENGTH_SHORT).show()
                                                    delay(1000)
                                                    actualViewModel.resetToNextPerson() 
                                                } 
                                            }, 
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    onShowQuestionnaire = { 
                                        Log.d("Questionnaire", "点击问卷调查按钮，当前问卷数量: ${uiState.questionnaires.size}")
                                        showQuestionnaireDialog = true 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showQuestionnaireDialog) {
            Log.d("Questionnaire", "正在显示问卷弹窗，问卷列表: ${uiState.questionnaires}")
            AlertDialog(
                onDismissRequest = { showQuestionnaireDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, null, modifier = Modifier.size(33.dp), tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(40.dp))
                        Text("问卷调查", fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    if (uiState.questionnaires.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("暂无问卷数据", fontSize = 24.sp, color = Color.Gray)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 600.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            uiState.questionnaires.forEach { question ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(22.dp)) {
                                        Row {
                                            if (question.isRequired) Text("* ", color = Color.Red, fontSize = 25.sp)
                                            Text(question.questionTitle, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34495E))
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            question.answerList.forEach { option ->
                                                val isSelected = uiState.answers[question.id] == option.id
                                                Surface(
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { actualViewModel?.selectAnswer(question.id, option.id) },
                                                    color = if (isSelected) Color(0xFF00796B) else Color(0xFFECF0F1),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFBDC3C7))
                                                ) {
                                                    Text(text = option.optionText, fontSize = 22.sp, color = if (isSelected) Color.White else Color(0xFF7F8C8D), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showQuestionnaireDialog = false },
                        modifier = Modifier.height(64.dp).width(160.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("完成", fontSize = 24.sp)
                    }
                },
                modifier = Modifier.width(900.dp)
            )
        }

        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
        ) {
            Surface(
                color = Color(0xFFFF5252),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.widthIn(min = 400.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CameraWithRoboflowPreview(
    cameraId: String = "4", 
    isBackOfHand: Boolean = false,
    modifier: Modifier = Modifier,
    onResult: (Boolean, String, Bitmap) -> Unit
) {
    var lastCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    Box(modifier = modifier) {
        CameraId4Preview(cameraId = cameraId, modifier = Modifier.fillMaxSize(), onFrameCaptured = { lastCapturedBitmap = it })
        LaunchedEffect(isBackOfHand) { 
            while(true) {
                delay(2000)
                lastCapturedBitmap?.let { rawBitmap ->
                    val processedBitmap = if (cameraId == "4") {
                        val matrix = android.graphics.Matrix().apply {
                            postRotate(-90f) 
                            postScale(-1f, 1f) 
                        }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else rawBitmap
                    
                    withContext(Dispatchers.IO) {
                        NailChecker.checkNail(processedBitmap, isBackOfHand) { isQualified, msg ->
                            if (isQualified) {
                                onResult(true, msg, processedBitmap)
                            } else {
                                onResult(false, msg, processedBitmap)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp")
@Composable
fun InspectionScreenPreview() {
    MaterialTheme {
        InspectionScreen(onBack = {})
    }
}
