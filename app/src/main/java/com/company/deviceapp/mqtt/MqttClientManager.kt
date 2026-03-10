package com.company.deviceapp.mqtt

import android.content.Context
import android.util.Log
import com.company.deviceapp.data.remote.api.MqConfigData
import kotlinx.coroutines.flow.MutableStateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

/**
 * 企业级 MQTT 长连接管理器
 * 负责与后端消息总线建立通信，接收人员下发、删除等实时指令
 */
class MqttClientManager(private val context: Context) {

    private var mqttClient: MqttAsyncClient? = null
    private val TAG = "MqttManager"

    // 使用 StateFlow 将连接状态实时推送到 UI 层（大屏底部状态栏）
    val connectionState = MutableStateFlow("未连接")

    /**
     * 根据接口下发的配置连接 MQTT
     */
    fun connect(config: MqConfigData) {
        try {
            // 1. 极其严谨的防空保护 (对应 V1.0.0 接口文档的 nullable 字段)
            val uris = config.uris
            if (uris.isNullOrEmpty()) {
                connectionState.value = "MQ地址为空"
                Log.e(TAG, "连接失败：配置中的 uris 列表为空")
                return
            }

            val deviceId = config.deviceId ?: "UNKNOWN_DEVICE"
            val brokerUrl = uris[0] // 获取数组中第一个 TCP 地址

            // 终端 ClientID 必须唯一，加上时间戳防止踢线
            val clientId = "Terminal_${deviceId}_${System.currentTimeMillis()}"

            // 2. 初始化纯 Java 版的 Paho Client，使用内存持久化
            mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())

            // 3. 配置连接参数
            val options = MqttConnectOptions().apply {
                userName = config.username ?: ""
                password = (config.password ?: "").toCharArray() // Paho 要求密码是 CharArray
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                isAutomaticReconnect = true // 企业级必须：断网重连机制
            }

            // 4. 设置全局回调监听器
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    connectionState.value = "连接断开, 正在重连..."
                    Log.e(TAG, "MQTT 连接意外断开: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        val payload = String(it.payload)
                        Log.d(TAG, "收到主题 [$topic] 下发的消息: $payload")
                        handleMessage(payload)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // QOS 1/2 消息送达确认，我们这里主要是作为接收端，暂不处理
                }
            })

            connectionState.value = "正在连接 MQ..."

            // 5. 真正发起异步连接
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    connectionState.value = "MQ已连接"
                    Log.d(TAG, "MQTT 连接服务器成功: $brokerUrl")

                    // 6. 连接成功后，严格按照蓝图文档订阅设备专属的 Person Topic
                    val topic = "/dewo/$deviceId/person"
                    try {
                        // 订阅该主题，QOS 设置为 1（至少送达一次）
                        mqttClient?.subscribe(topic, 1)
                        Log.d(TAG, "成功订阅人员变更主题: $topic")
                    } catch (e: Exception) {
                        Log.e(TAG, "订阅主题失败: $topic", e)
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    connectionState.value = "MQ连接失败"
                    Log.e(TAG, "MQTT 连接服务器失败", exception)
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "MQTT 客户端初始化异常", e)
            connectionState.value = "MQ组件异常"
        }
    }

    /**
     * 严格按照蓝图架构解析指令
     * cmd: 7(删除全部) / 8(删除人员) / 9(新增) / 10(修改)
     */
    private fun handleMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            val cmd = json.optInt("cmd", -1)

            when (cmd) {
                7 -> {
                    Log.d(TAG, ">>> 业务指令 7：清空本地人员库 (Delete All)")
                    // TODO: 调用 PersonnelDao.deleteAllPersonnel()
                }
                8 -> {
                    val personId = json.optString("personId", "")
                    Log.d(TAG, ">>> 业务指令 8：删除特定人员 (Delete Person), ID: $personId")
                    // TODO: 调用 PersonnelDao.deletePersonnelById(personId)
                }
                9 -> {
                    Log.d(TAG, ">>> 业务指令 9：新增人员信息 (Add Person) - 解析 JSON 并插入 Room 数据库")
                    // TODO: 解析出 PersonnelEntity 并调用 PersonnelDao.insertOrUpdatePersonnel()
                }
                10 -> {
                    Log.d(TAG, ">>> 业务指令 10：更新人员信息 (Update Person) - 覆盖 Room 数据库记录")
                    // TODO: 解析出 PersonnelEntity 并调用 PersonnelDao.insertOrUpdatePersonnel()
                }
                else -> {
                    Log.w(TAG, "收到未知业务指令 cmd: $cmd")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "下发消息的 JSON 解析失败", e)
        }
    }

    /**
     * 手动断开连接并释放资源（通常在 App 退出时调用）
     */
    fun disconnect() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
            }
            mqttClient = null
            connectionState.value = "未连接"
            Log.d(TAG, "MQTT 已手动断开释放")
        } catch (e: Exception) {
            Log.e(TAG, "断开 MQTT 连接时发生异常", e)
        }
    }
}