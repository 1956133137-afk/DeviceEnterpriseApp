package com.company.deviceapp

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.deviceapp.data.remote.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 首页 UI 状态模型
data class HomeUiState(
    val currentUrl: String = "http://192.168.1.100:8080/", // 默认配置地址
    val deviceSn: String = "YNDEV_20260310",              // 模拟彦诺设备 SN 码
    val heartbeatStatus: String = "等待初始化",
    val heartbeatColor: Color = Color(0xFF90A4AE),        // 灰色
    val mqttStatus: String = "等待心跳",
    val mqttColor: Color = Color(0xFF90A4AE)              // 灰色
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 严格按文档流程：2.设备心跳 -> 3.获取MQ服务地址
    fun startDeviceInitialization() {
        val state = _uiState.value
        val baseUrl = state.currentUrl
        val sn = state.deviceSn

        _uiState.update { it.copy(heartbeatStatus = "心跳连接中...", heartbeatColor = Color(0xFFFBC02D)) }

        viewModelScope.launch {
            try {
                // 构建动态 URL 的网络请求客户端
                val apiService = RetrofitClient.create(baseUrl)

                // 【步骤 2】: 触发设备心跳
                // 文档说明：type=4 代表晨检机/终端，传入 serialNum
                val heartbeatResponse = apiService.heartbeat(type = 4, serialNum = sn)

                if (heartbeatResponse.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            heartbeatStatus = "心跳正常",
                            heartbeatColor = Color(0xFF2E7D32), // 绿色
                            mqttStatus = "获取 MQ 配置中...",
                            mqttColor = Color(0xFFFBC02D) // 黄色
                        )
                    }

                    // 【步骤 3】: 心跳正常后，触发获取 MQ 服务地址
                    val mqResponse = apiService.getMqConfig(serialNumber = sn)

                    if (mqResponse.isSuccessful && mqResponse.data != null) {
                        _uiState.update {
                            it.copy(
                                mqttStatus = "配置已获取, 准备连接",
                                mqttColor = Color(0xFF0277BD) // 蓝色，表示准备就绪
                            )
                        }
                        // TODO: 下一步将把 mqResponse.data 丢给 Paho MQTT Service 建立长连接
                    } else {
                        _uiState.update { it.copy(mqttStatus = "MQ 配置获取失败", mqttColor = Color(0xFFD32F2F)) }
                    }
                } else {
                    _uiState.update { it.copy(heartbeatStatus = "心跳被拒: ${heartbeatResponse.msg}", heartbeatColor = Color(0xFFD32F2F)) }
                }
            } catch (e: Exception) {
                // 企业级异常拦截：网络不通或后端没开时的处理，防止应用崩溃
                _uiState.update {
                    it.copy(
                        heartbeatStatus = "网络异常或超时",
                        heartbeatColor = Color(0xFFD32F2F), // 红色
                        mqttStatus = "流程中断",
                        mqttColor = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}