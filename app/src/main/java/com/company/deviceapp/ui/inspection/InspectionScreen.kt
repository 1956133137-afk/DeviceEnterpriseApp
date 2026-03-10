package com.company.deviceapp.ui.inspection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InspectionScreen(
    onBack: () -> Unit
    // viewModel: InspectionViewModel = hiltViewModel()
) {
    // 模拟状态，实际项目中直接 collectAsState 取 viewModel.uiState
    var currentStep by remember { mutableStateOf(InspectionStep.WAITING_FACE_LOGIN) }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFECEFF1))) {

        // ==========================================
        // 左侧：视觉捕获区 (占宽 50%)
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Black), // 黑色背景模拟 CameraX 预览层
            contentAlignment = Alignment.Center
        ) {
            if (currentStep == InspectionStep.WAITING_FACE_LOGIN) {
                Text("CameraX 人脸捕获区...", color = Color.White, fontSize = 32.sp)

                // 模拟人脸识别成功测试按钮
                Button(
                    onClick = { currentStep = InspectionStep.DOING_INSPECTION },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                ) {
                    Text("模拟：识别到员工 [张三]")
                }
            } else {
                Text("请将双手置于检测区域...", color = Color.Green, fontSize = 32.sp)
            }
        }

        // ==========================================
        // 右侧：业务交互区 (占宽 50%)
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            // 顶部返回按钮
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("退出晨检", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (currentStep == InspectionStep.WAITING_FACE_LOGIN) {
                // 待机状态
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("请正对屏幕进行人脸登录", fontSize = 48.sp, color = Color(0xFF546E7A), fontWeight = FontWeight.Bold)
                }
            } else {
                // 已登录，展示业务信息
                Text("当前人员：张三 (工号: 111111)", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                Spacer(modifier = Modifier.height(16.dp))

                // 上次晨检记录提示
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Text(
                        "您今日最近一次晨检于 08:30 完成，结果：【正常】",
                        fontSize = 24.sp,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("今日问卷调查：", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // 模拟问卷 UI (实际通过 LazyColumn 渲染 questionnaires 数据)
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("1. 您今日是否有发热、咳嗽等症状？", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            RadioButton(selected = true, onClick = {})
                            Text("否", fontSize = 26.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(32.dp))
                            RadioButton(selected = false, onClick = {})
                            Text("是", fontSize = 26.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 提交按钮
                Button(
                    onClick = {
                        currentStep = InspectionStep.WAITING_FACE_LOGIN
                        // 实际业务这里调用 viewModel.uploadInspectionRecord()
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("完成手部检测并提交晨检", fontSize = 32.sp)
                }
            }
        }
    }
}