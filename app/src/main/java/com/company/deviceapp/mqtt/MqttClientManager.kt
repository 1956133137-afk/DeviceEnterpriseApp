package com.company.deviceapp.mqtt

import android.content.Context
import android.util.Log
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.MqConfigData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

class MqttClientManager(
    private val context: Context,
    private val personnelDao: PersonnelDao
) {
    private var mqttClient: MqttAsyncClient? = null
    private val TAG = "MqttManager"

    // 将 MQTT 连接状态实时推送到大屏底部 UI
    val connectionState = MutableStateFlow("未连接")

    // 核心设计：单独的子线程协程作用域。
    // 确保收到几十上百条人员下发指令时，数据库读写操作在 IO 线程进行，绝对不卡顿！
    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun connect(config: MqConfigData) {
        try {
            val uris = config.uris
            if (uris.isNullOrEmpty()) {
                connectionState.value = "MQ地址为空"
                Log.e(TAG, "连接失败：配置中的 uris 列表为空")
                return
            }

            val deviceId = config.deviceId ?: "UNKNOWN_DEVICE"
            val brokerUrl = uris[0]
            val clientId = "Terminal_${deviceId}_${System.currentTimeMillis()}"

            mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                userName = config.username ?: ""
                password = (config.password ?: "").toCharArray()
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                isAutomaticReconnect = true // 断网自动重连
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    connectionState.value = "连接断开, 正在重连..."
                    Log.e(TAG, "MQTT 连接意外断开: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        val payload = String(it.payload)
                        Log.d(TAG, "收到主题 [$topic] 下发的消息: $payload")
                        handleMessage(payload) // 将收到的 JSON 交给分发器
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            connectionState.value = "正在连接 MQ..."

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    connectionState.value = "MQ已连接"
                    Log.d(TAG, "MQTT 连接服务器成功: $brokerUrl")

                    val topic = "/dewo/$deviceId/person"
                    try {
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
     * 核心业务：边缘计算的基石，负责将云端的人员数据异步落地到本地 SQLite
     */
    private fun handleMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            val cmd = json.optInt("cmd", -1)

            // 切换到 IO 线程执行数据库操作
            dbScope.launch {
                when (cmd) {
                    7 -> {
                        Log.d(TAG, ">>> 指令 7：清空本地人员库")
                        personnelDao.deleteAllPersonnel()
                    }
                    8 -> {
                        val personId = json.optString("personnelId", "")
                        if (personId.isNotEmpty()) {
                            Log.d(TAG, ">>> 指令 8：删除特定人员, ID: $personId")
                            personnelDao.deletePersonnelById(personId)
                        }
                    }
                    9, 10 -> {
                        val personnelId = json.optString("personnelId", "")
                        if (personnelId.isNotEmpty()) {
                            Log.d(TAG, ">>> 指令 $cmd：新增/修改人员信息，准备落库")

                            //  极其严格地对齐你源码中的 PersonnelEntity 字段
                            // 除了 personnelId，其他字段使用 optString 并允许返回 null 以适配 String? 类型
                            val entity = PersonnelEntity(
                                personnelId = personnelId,
                                name = if (json.has("name")) json.optString("name") else null,
                                faceFeatureImgPath = if (json.has("faceFeatureImgPath")) json.optString("faceFeatureImgPath") else null,
                                icNum = if (json.has("icNum")) json.optString("icNum") else null,
                                jobNum = if (json.has("jobNum")) json.optString("jobNum") else null, // 源码独有字段
                                memberType = if (json.has("memberType")) json.optString("memberType") else null,
                                personnelType = if (json.has("personnelType")) json.optString("personnelType") else null,
                                phone = if (json.has("phone")) json.optString("phone") else null,
                                healthCard = if (json.has("healthCard")) json.optString("healthCard") else null,
                                healthCardTime = if (json.has("healthCardTime")) json.optString("healthCardTime") else null
                            )

                            // 保存或更新到本地 Room 数据库
                            personnelDao.insertOrUpdatePersonnel(entity)
                            Log.d(TAG, " 边缘同步成功！人员 [${entity.name}] (卡号: ${entity.icNum}) 已写入本地数据库，可断网秒刷！")

                            Log.d(TAG, "🔍 检查解析的实体对象: $entity")
                            personnelDao.insertOrUpdatePersonnel(entity)

                            val checkUser = personnelDao.getPersonnelById(entity.personnelId)
                            Log.d(TAG, "🕵️ 数据库反查验证：成功从硬盘物理读取 -> 查到的姓名: ${checkUser?.name}, 卡号: ${checkUser?.icNum}")
                        } else {
                            Log.e(TAG, " 人员下发失败：JSON 中缺失核心主键 personnelId")
                        }
                    }
                    else -> {
                        Log.w(TAG, "收到未知业务指令 cmd: $cmd")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "下发消息的 JSON 解析或落库失败", e)
        }
    }

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