package com.company.deviceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel // 用于获取 ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFECEFF1)
                ) {
                    EnterpriseHomeScreen()
                }
            }
        }
    }
}

@Composable
fun EnterpriseHomeScreen(
    viewModel: HomeViewModel = viewModel() // 挂载刚写好的 ViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 观察 ViewModel 中的状态
    val uiState by viewModel.uiState.collectAsState()

    var currentTime by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }

    // 1. 时钟更新
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    // 2. 应用启动自动触发：设备心跳与获取 MQ (严格执行文档流程)
    LaunchedEffect(Unit) {
        viewModel.startDeviceInitialization()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 顶部导航栏 (Header)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .shadow(elevation = 8.dp)
                    .background(Color(0xFF0D47A1))
                    .padding(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("YN", color = Color(0xFF0D47A1), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "彦诺智慧食堂终端",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = currentTime,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 28.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            // ==========================================
            // 核心业务卡片区
            // ==========================================
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnterpriseModuleCard(
                        icon = Icons.Default.AccountBox,
                        title = "晨检业务系统",
                        subtitle = "健康监测终端工作台",
                        statusText = "服务待命",
                        accentColor = Color(0xFF1976D2),
                        onClick = {
                            Toast.makeText(context, "启动晨检业务模块...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.width(120.dp))

                    EnterpriseModuleCard(
                        icon = Icons.Default.List,
                        title = "留样柜系统",
                        subtitle = "食品留样终端工作台",
                        statusText = "服务待命",
                        accentColor = Color(0xFF00796B),
                        onClick = {
                            Toast.makeText(context, "启动留样柜业务模块...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ==========================================
            // 底部状态栏 (动态绑定网络状态)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White)
                    .padding(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Device Info", tint = Color(0xFF546E7A), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    DiagnosticItem("设备 SN", uiState.deviceSn)
                    DiagnosticDivider()
                    DiagnosticItem("环境 URL", uiState.currentUrl)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态指示灯与 ViewModel 双向绑定
                    StatusIndicator("心跳服务", uiState.heartbeatStatus, uiState.heartbeatColor)
                    Spacer(modifier = Modifier.width(40.dp))
                    StatusIndicator("MQTT 通信", uiState.mqttStatus, uiState.mqttColor)
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(text = "版本: V1.0.4", fontSize = 22.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Medium)
                }
            }
        }

        // ==========================================
        // 隐藏的“陌生人”入口
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(150.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val currentClickTime = System.currentTimeMillis()
                    if (currentClickTime - lastClickTime < 800) {
                        clickCount++
                        if (clickCount >= 4) {
                            clickCount = 0
                            showConfigDialog = true
                        }
                    } else {
                        clickCount = 1
                    }
                    lastClickTime = currentClickTime
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "陌生人", fontSize = 1.sp, color = Color.Transparent)
        }
    }

    // ==========================================
    // 后台工程配置弹窗
    // ==========================================
    if (showConfigDialog) {
        var urlInput by remember { mutableStateOf(uiState.currentUrl) }
        var passwordInput by remember { mutableStateOf("") }
        var isPasswordError by remember { mutableStateOf(false) }
        val ADMIN_PASSWORD = "123456"

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF0D47A1))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("终端工程模式配置", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                }
            },
            text = {
                Column {
                    Text("警告：非实施工程师请勿修改此配置。", fontSize = 24.sp, color = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.height(40.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            isPasswordError = false
                        },
                        label = { Text("请输入管理密码", fontSize = 26.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", modifier = Modifier.size(32.dp)) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isPasswordError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 32.sp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("API Base URL (必须以 / 结尾)", fontSize = 26.sp) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = "URL", modifier = Modifier.size(32.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 32.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (passwordInput != ADMIN_PASSWORD) {
                                isPasswordError = true
                                Toast.makeText(context, "密码错误，拒绝访问", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            var finalUrl = urlInput.trim()
                            if (!finalUrl.endsWith("/")) finalUrl += "/"

                            showConfigDialog = false
                            Toast.makeText(context, "配置已写入终端存储，正在执行软重启...", Toast.LENGTH_LONG).show()

                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        }
                    },
                    modifier = Modifier.padding(16.dp).height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("写入配置并重启设备", fontSize = 28.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfigDialog = false },
                    modifier = Modifier.padding(16.dp).height(80.dp)
                ) {
                    Text("放弃修改", fontSize = 28.sp)
                }
            },
            modifier = Modifier.width(1000.dp),
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

// --- 企业级卡片组件 ---
@Composable
fun EnterpriseModuleCard(
    icon: ImageVector, title: String, subtitle: String, statusText: String, accentColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(width = 640.dp, height = 520.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(accentColor))
            Column(
                modifier = Modifier.fillMaxSize().padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(100.dp), tint = accentColor)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = title, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF263238))
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = subtitle, fontSize = 26.sp, color = Color(0xFF546E7A))
                Spacer(modifier = Modifier.height(64.dp))
                Surface(
                    shape = CircleShape, color = accentColor.copy(alpha = 0.1f), modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = statusText, color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 底部运维状态小组件 ---
@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", fontSize = 24.sp, color = Color(0xFF78909C), fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 24.sp, color = Color(0xFF263238), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiagnosticDivider() {
    Box(modifier = Modifier.padding(horizontal = 32.dp).width(2.dp).height(32.dp).background(Color(0xFFCFD8DC)))
}

@Composable
fun StatusIndicator(label: String, status: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", fontSize = 24.sp, color = Color(0xFF78909C))
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = status, fontSize = 24.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Preview(name = "彦诺 工业级设备终端", widthDp = 1920, heightDp = 1080, showBackground = true)
@Composable
fun EnterpriseHomeScreenPreview() {
    MaterialTheme {
        EnterpriseHomeScreen()
    }
}