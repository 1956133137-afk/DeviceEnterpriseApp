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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.deviceapp.ui.inspection.InspectionScreen
import com.company.deviceapp.ui.sample.SampleCabinetScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFECEFF1)
                ) {
                    DeviceAppNavigation()
                }
            }
        }
    }
}

@Composable
fun DeviceAppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            EnterpriseHomeScreen(navController = navController)
        }
        composable("inspection") {
            InspectionScreen(onBack = { 
                if (navController.currentBackStackEntry?.destination?.route == "inspection") {
                    navController.popBackStack() 
                }
            })
        }
        composable("sample_cabinet") {
            SampleCabinetScreen(onBack = { 
                if (navController.currentBackStackEntry?.destination?.route == "sample_cabinet") {
                    navController.popBackStack() 
                }
            })
        }
    }
}

@Composable
fun EnterpriseHomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentTime by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var clickCount by remember { mutableLongStateOf(0L) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    var lastModuleClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startDeviceInitialization()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    EnterpriseModuleCard(
                        icon = Icons.Default.AccountBox,
                        title = "晨检业务系统",
                        subtitle = "健康监测终端工作台",
                        statusText = "服务待命",
                        accentColor = Color(0xFF1976D2),
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastModuleClickTime > 2000) {
                                lastModuleClickTime = now
                                if (navController.currentBackStackEntry?.destination?.route == "home") {
                                    navController.navigate("inspection")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(120.dp))

                    EnterpriseModuleCard(
                        icon = Icons.Default.List,
                        title = "留样业务系统",
                        subtitle = "食品留样终端工作台",
                        statusText = "准备就绪",
                        accentColor = Color(0xFF00796B),
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastModuleClickTime > 2000) {
                                lastModuleClickTime = now
                                if (navController.currentBackStackEntry?.destination?.route == "home") {
                                    navController.navigate("sample_cabinet")
                                }
                            }
                        }
                    )
                }
            }

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
                    StatusIndicator("心跳服务", uiState.heartbeatStatus, uiState.heartbeatColor)
                    Spacer(modifier = Modifier.width(40.dp))
                    StatusIndicator("MQTT 通信", uiState.mqttStatus, uiState.mqttColor)
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(text = "版本: V1.0.0", fontSize = 22.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Medium)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(100.dp)
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
            Text(text = "", fontSize = 1.sp, color = Color.Transparent)
        }

        if (showConfigDialog) {
            var urlInput by remember { mutableStateOf(uiState.currentUrl) }
            var passwordInput by remember { mutableStateOf("") }
            var isPasswordError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showConfigDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(64.dp), tint = Color(0xFF0D47A1))
                        Spacer(modifier = Modifier.width(20.dp))
                        Text("终端工程模式配置", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                isPasswordError = false
                            },
                            label = { Text("请输入管理密码", fontSize = 28.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp),
                            visualTransformation = PasswordVisualTransformation(),
                            isError = isPasswordError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("API Base URL (必须以 / 结尾)", fontSize = 28.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 终端密码
                            if (passwordInput != "123456") {
                                isPasswordError = true
                                Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            var finalUrl = urlInput.trim()
                            if (!finalUrl.endsWith("/")) finalUrl += "/"

                            val sharedPrefs = context.getSharedPreferences("device_config", android.content.Context.MODE_PRIVATE)
                            sharedPrefs.edit().putString("BASE_URL", finalUrl).commit()

                            showConfigDialog = false
                            Toast.makeText(context, "配置已永久写入，正在重启...", Toast.LENGTH_LONG).show()

                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        },
                        modifier = Modifier.height(90.dp).padding(horizontal = 16.dp)
                    ) {
                        Text("保存并重启设备", fontSize = 32.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showConfigDialog = false },
                        modifier = Modifier.height(90.dp).padding(horizontal = 16.dp)
                    ) {
                        Text("取消", fontSize = 32.sp)
                    }
                },
                modifier = Modifier.width(1100.dp)
            )
        }
    }
}

@Composable
fun EnterpriseModuleCard(
    icon: ImageVector, title: String, subtitle: String, statusText: String, accentColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(width = 640.dp, height = 520.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
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
                Surface(shape = CircleShape, color = accentColor.copy(alpha = 0.1f), modifier = Modifier.wrapContentSize()) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = statusText, color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
