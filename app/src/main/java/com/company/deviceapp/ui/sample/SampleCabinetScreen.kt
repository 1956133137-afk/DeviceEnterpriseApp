package com.company.deviceapp.ui.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.deviceapp.R
import com.company.deviceapp.ui.inspection.FaceSdkPreviewArea
import com.company.deviceapp.ui.inspection.CardLoginPreviewArea
import com.company.deviceapp.ui.inspection.CameraId4Preview
import com.yannuo.library.interfaces.QrCodeListener
import com.yannuo.library.interfaces.SerialPortListener
import com.yannuo.library.serialPortHelper.WeighHelper
import com.yannuo.library.interfaces.WeighListener
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleCabinetScreen(
    onBack: () -> Unit,
    viewModel: SampleCabinetViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val actualViewModel: SampleCabinetViewModel? = if (isPreview) null else (viewModel ?: hiltViewModel())
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by if (actualViewModel != null) {
        actualViewModel.uiState.collectAsState()
    } else {
        remember { mutableStateOf(SampleCabinetUiState()) }
    }

    val viewModelRef = remember(actualViewModel) { WeakReference(actualViewModel) }

    var faceLoginActive by remember { mutableStateOf(false) }
    var faceLoginSession by remember { mutableStateOf(0) }
    var cardLoginActive by remember { mutableStateOf(false) }
    
    var isCameraActive by remember { mutableStateOf(false) }

    var dishMenuExpanded by remember { mutableStateOf(false) }
    var mealMenuExpanded by remember { mutableStateOf(false) }

    if (uiState.currentStep == SampleCabinetStep.WAITING_LOGIN && !isPreview) {
        DisposableEffect(Unit) {
            val qrHelper = QrCodeHelper.getInstance()
            val serialHelper = SerialPortHelper()
            serialHelper.setSerialPort(1)

            val handlerThread = android.os.HandlerThread("HardwareThreadSample").apply { start() }
            val hardwareDispatcher = android.os.Handler(handlerThread.looper).asCoroutineDispatcher()

            val job = coroutineScope.launch(hardwareDispatcher) {
                try {
                    Runtime.getRuntime().exec("su -c chmod 666 /dev/ttyS0")
                    Runtime.getRuntime().exec("su -c chmod 666 /dev/ttyS4")

                    qrHelper.setUSBorCOM("/dev/ttyS0", 4800, true)
                    qrHelper.open(context.applicationContext, object : QrCodeListener {
                        override fun onQrCodeSuccess() {}
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
                        override fun onSerialPortSuccess() { serialHelper.startSerialPort() }
                        override fun onSerialPortReceived(dataStr: String, data: ByteArray, size: Int) {
                            viewModelRef.get()?.let { vm ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    vm.onCardOrQrRecognized(dataStr)
                                }
                            }
                        }
                        override fun onSerialPortException(msg: String) {}
                        override fun onSerialPortFailure(msg: String) {}
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
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val weighHelper = remember { WeighHelper() }
    DisposableEffect(Unit) {
        if (!isPreview) {
            try {
                Runtime.getRuntime().exec("su -c chmod 666 /dev/ttyXRUSB0")
                weighHelper.openWeigh("/dev/ttyXRUSB0", 9600, object : WeighListener {
                    override fun onWeighReceived(weight: String, status: Int, msg: String) {
                        actualViewModel?.updateWeight(weight)
                    }
                    override fun onWeighTips(code: Int, message: String) {
                        Log.d("Weight", "Code: $code, Msg: $message")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            if (!isPreview) {
                try { weighHelper.releaseWeigh() } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // 图片流加载
    val capturedBitmap by produceState<Bitmap?>(initialValue = null, uiState.capturedImgPath) {
        value = null
        val path = uiState.capturedImgPath
        if (!path.isNullOrBlank()) {
            repeat(5) {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        val matrix = android.graphics.Matrix().apply {
                            postRotate(-90f)
                            postScale(-1f, 1f)
                        }
                        value = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        return@produceState
                    }
                }
                delay(200)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // 左侧视觉区
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
                when {
                    faceLoginActive -> {
                        FaceSdkPreviewArea(
                            sessionId = faceLoginSession,
                            modifier = Modifier.fillMaxSize(),
                            onRecognizedSuccess = { faceToken, _, _ ->
                                faceLoginActive = false
                                actualViewModel?.onFaceRecognized(faceToken)
                            },
                            onClose = { faceLoginActive = false }
                        )
                    }
                    cardLoginActive -> {
                        CardLoginPreviewArea(
                            modifier = Modifier.fillMaxSize(),
                            onClose = { cardLoginActive = false }
                        )
                    }
                    isCameraActive -> {
                        var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraId4Preview(
                                cameraId = "4",
                                modifier = Modifier.fillMaxSize(),
                                onFrameCaptured = { currentBitmap = it }
                            )
                            Button(
                                onClick = {
                                    currentBitmap?.let { bitmap ->
                                        val fileName = "sample_${System.currentTimeMillis()}.jpg"
                                        val file = File(context.cacheDir, fileName)
                                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                        actualViewModel?.onPhotoCaptured(file.absolutePath)
                                        isCameraActive = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).height(80.dp).width(200.dp),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text("点击拍照", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_plate),
                                contentDescription = null,
                                tint = Color.Unspecified, 
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("智慧留样影像中心", color = Color.Gray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!faceLoginActive && !cardLoginActive && !isCameraActive) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF00E676)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("系统就绪", fontSize = 22.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1.25f) 
                .fillMaxHeight()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f)
                    
                    Surface(
                        onClick = onBack,
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
                    Text("智能留样控制台", fontSize = 44.sp, fontWeight = FontWeight.Black)
                }
                
                if (uiState.currentStep == SampleCabinetStep.WAITING_LOGIN) {
                    Surface(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                cardLoginActive = false
                                isCameraActive = false
                                actualViewModel?.resetToNextSample()
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

            Spacer(modifier = Modifier.height(40.dp))

            if (uiState.currentStep == SampleCabinetStep.WAITING_LOGIN && !faceLoginActive && !cardLoginActive) {
                Box(modifier = Modifier.fillMaxWidth().height(950.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Color.LightGray, modifier = Modifier.size(100.dp))
                        Text("终端待机中", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF95A5A6))
                        Text("请先刷卡或者扫码登录", fontSize = 44.sp, color = Color(0xFFFF7043))
                    }
                }
            } else if (uiState.currentStep != SampleCabinetStep.WAITING_LOGIN) {
                Card(
                    shape = RoundedCornerShape(24.dp), 
                    colors = CardDefaults.cardColors(containerColor = Color.White), 
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally 
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "操作人员：${uiState.operatorName ?: "未登录"}",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1565C0),
                                    letterSpacing = 1.sp
                                )
                                
                                Spacer(modifier = Modifier.width(48.dp)) 

                                Text(
                                    text = "类型：",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF546E7A)
                                )

                                StatusChip(
                                    text = "留样",
                                    isSelected = uiState.status == "1",
                                    onClick = { actualViewModel?.setStatus("1") }
                                )

                                Spacer(modifier = Modifier.width(12.dp)) 

                                StatusChip(
                                    text = "取样",
                                    isSelected = uiState.status == "2",
                                    onClick = { actualViewModel?.setStatus("2") }
                                )
                            }

                            OutlinedTextField(
                                value = uiState.weightInput,
                                onValueChange = {},
                                readOnly = true, 
                                label = { Text("实时称重", fontSize = 16.sp) },
                                modifier = Modifier.width(180.dp).height(64.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f) 
                                .aspectRatio(16f / 10f) 
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F3F4))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .clickable { isCameraActive = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedBitmap != null) {
                                Image(bitmap = capturedBitmap!!.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_plate),
                                        null, 
                                        tint = Color.Gray, 
                                        modifier = Modifier.size(64.dp)
                                    ) 
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("点击样品拍照", color = Color.Gray, fontSize = 25.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ExposedDropdownMenuBox(
                            expanded = dishMenuExpanded,
                            onExpandedChange = { dishMenuExpanded = !dishMenuExpanded },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedDish?.dishName ?: "请选择菜品",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("必填菜品", fontSize = 20.sp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(72.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, textAlign = TextAlign.Center),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dishMenuExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = dishMenuExpanded,
                                onDismissRequest = { dishMenuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (uiState.dishList.isEmpty() && !uiState.isLoading) {
                                    DropdownMenuItem(text = { Text("暂无数据，正在加载...", fontSize = 23.sp, color = Color.Gray) }, onClick = { })
                                }
                                uiState.dishList.forEach { dish ->
                                    DropdownMenuItem(
                                        text = { Text(dish.dishName, fontSize = 22.sp, modifier = Modifier.padding(vertical = 4.dp)) },
                                        onClick = {
                                            actualViewModel?.selectDish(dish)
                                            dishMenuExpanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = mealMenuExpanded,
                            onExpandedChange = { mealMenuExpanded = !mealMenuExpanded },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedMeal?.dictLabel ?: "请选择餐次",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("必填餐次", fontSize = 20.sp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(72.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, textAlign = TextAlign.Center),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealMenuExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = mealMenuExpanded,
                                onDismissRequest = { mealMenuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (uiState.mealList.isEmpty() && !uiState.isLoading) {
                                    DropdownMenuItem(text = { Text("暂无数据，正在加载...", fontSize = 23.sp, color = Color.Gray) }, onClick = { })
                                }
                                uiState.mealList.forEach { meal ->
                                    DropdownMenuItem(
                                        text = { Text(meal.dictLabel, fontSize = 22.sp, modifier = Modifier.padding(vertical = 4.dp)) },
                                        onClick = {
                                            actualViewModel?.selectMeal(meal)
                                            mealMenuExpanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val parsedWeight = uiState.weightInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        val isFormValid = uiState.selectedDish != null && 
                                          uiState.selectedMeal != null && 
                                          uiState.capturedImgPath != null && 
                                          parsedWeight > 0.1

                        Button(
                            onClick = { actualViewModel?.submitSampleRecord() },
                            modifier = Modifier.fillMaxWidth(0.75f).height(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isFormValid && !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color.White)
                            } else {
                                Text("确认提交登记", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF1565C0) else Color(0xFFF5F7FA),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFCFD8DC)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 22.sp,
            color = if (isSelected) Color.White else Color(0xFF546E7A),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp")
@Composable
fun SampleCabinetScreenPreview() {
    MaterialTheme {
        SampleCabinetScreen(onBack = {})
    }
}
