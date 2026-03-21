package com.company.deviceapp

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.deviceapp.data.remote.api.RetrofitClient
import com.company.deviceapp.mqtt.MqttClientManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val currentUrl: String = "",
    val deviceSn: String = "DWDEV20230515014A",
    val heartbeatStatus: String = "等待初始化",
    val heartbeatColor: Color = Color(0xFF90A4AE),
    val mqttStatus: String = "等待心跳",
    val mqttColor: Color = Color(0xFF90A4AE)
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mqttManager: MqttClientManager,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val savedUrl = sharedPrefs.getString("BASE_URL", "http://192.168.2.8:8080/") ?: "http://192.168.1.100:8080/"
        _uiState.update { it.copy(currentUrl = savedUrl) }

        viewModelScope.launch {
            mqttManager.connectionState.collect { state ->
                val color = when (state) {
                    "MQ已连接" -> Color(0xFF2E7D32)
                    "正在连接 MQ..." -> Color(0xFFFBC02D)
                    else -> Color(0xFFD32F2F)
                }
                _uiState.update { it.copy(mqttStatus = state, mqttColor = color) }
            }
        }
    }

    fun startDeviceInitialization() {
        val state = _uiState.value
        val baseUrl = state.currentUrl
        val sn = state.deviceSn

        _uiState.update { it.copy(heartbeatStatus = "心跳连接中...", heartbeatColor = Color(0xFFFBC02D)) }

        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.create(baseUrl)
                val heartbeatResponse = apiService.heartbeat(type = 4, serialNum = sn)

                if (heartbeatResponse.isSuccessful) {
                    _uiState.update { it.copy(heartbeatStatus = "心跳正常", heartbeatColor = Color(0xFF2E7D32)) }

                    val mqResponse = apiService.getMqConfig(serialNumber = sn)
                    if (mqResponse.isSuccessful && mqResponse.data != null) {
                        mqttManager.connect(mqResponse.data)
                    } else {
                        android.util.Log.e("MqError", "MQ配置失败，后台返回的内容: code=${mqResponse.code}, msg=${mqResponse.message}, data=${mqResponse.data}")

                        _uiState.update {
                            it.copy(
                                mqttStatus = "获取失败: ${mqResponse.message ?: "空数据"}",
                                mqttColor = Color(0xFFD32F2F)
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(heartbeatStatus = "心跳被拒", heartbeatColor = Color(0xFFD32F2F)) }
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkError", "心跳失败原因：", e)
                _uiState.update {
                    it.copy(heartbeatStatus = "网络异常", heartbeatColor = Color(0xFFD32F2F))
                }
            }
        }
    }
}