package com.company.deviceapp.mqtt

import android.content.Context
import android.util.Log
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.MqConfigData
import com.yannuo.library.faceHelper.FaceSDKHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MqttClientManager(
    private val context: Context,
    private val personnelDao: PersonnelDao
) {
    private var mqttClient: MqttAsyncClient? = null
    private val TAG = "MqttManager"

    val connectionState = MutableStateFlow("未连接")

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
                isAutomaticReconnect = true
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
                        handleMessage(payload)
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


    private fun ensureFaceSdkReady(onReady: () -> Unit) {
        FaceSDKHandler.getInstance().initFaceSDK(
            context.applicationContext,
            "inspection_group",
            context.applicationContext.filesDir.absolutePath
        ) { code, message ->
            if (code == 0) {
                Log.d(TAG, "FaceSDK 初始化成功")
                onReady()
            } else {
                Log.e(TAG, "FaceSDK 初始化失败: $message")
            }
        }
    }

    private fun downloadImageToLocal(imageUrl: String, personnelId: String): String? {
        return try {
            val faceDir = File(context.filesDir, "mqtt_faces")
            if (!faceDir.exists()) {
                faceDir.mkdirs()
            }

            val localFile = File(faceDir, "${personnelId}.jpg")
            URL(imageUrl).openStream().use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            localFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "下载人脸图片失败: $imageUrl", e)
            null
        }
    }

    private fun registerFaceAndGetToken(faceImagePath: String): String? {
        return try {
            val faceToken = FaceSDKHandler.getInstance().registerFace(faceImagePath)
            if (faceToken.isNotEmpty()) {
                val bindOk = FaceSDKHandler.getInstance().bindFaceToGroup(faceToken)
                if (bindOk) {
                    Log.d(TAG, "本地人脸注册并绑定成功, token=$faceToken")
                    faceToken
                } else {
                    Log.e(TAG, "本地人脸绑定失败")
                    null
                }
            } else {
                Log.e(TAG, "本地人脸注册失败，返回空 token")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerFace 异常", e)
            null
        }
    }



    private fun handleMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            val cmd = json.optInt("cmd", -1)

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
                            Log.d(TAG, ">>> 指令 $cmd：新增/修改人员信息，准备落库并注册本地人脸库")

                            val faceImageUrl =
                                if (json.has("faceFeatureImgPath")) json.optString("faceFeatureImgPath") else null

                            ensureFaceSdkReady {
                                dbScope.launch {
                                    val localPath = if (!faceImageUrl.isNullOrBlank()) {
                                        downloadImageToLocal(faceImageUrl, personnelId)
                                    } else {
                                        null
                                    }

                                    val faceToken = if (!localPath.isNullOrBlank()) {
                                        registerFaceAndGetToken(localPath)
                                    } else {
                                        null
                                    }

                                    val entity = PersonnelEntity(
                                        personnelId = personnelId,
                                        name = if (json.has("name")) json.optString("name") else null,
                                        faceFeatureImgPath = faceImageUrl,
                                        faceToken = faceToken,
                                        icNum = if (json.has("icNum")) json.optString("icNum") else null,
                                        jobNum = if (json.has("jobNum")) json.optString("jobNum") else null,
                                        memberType = if (json.has("memberType")) json.optString("memberType") else null,
                                        personnelType = if (json.has("personnelType")) json.optString("personnelType") else null,
                                        phone = if (json.has("phone")) json.optString("phone") else null,
                                        healthCard = if (json.has("healthCard")) json.optString("healthCard") else null,
                                        healthCardTime = if (json.has("healthCardTime")) json.optString("healthCardTime") else null
                                    )

                                    personnelDao.insertOrUpdatePersonnel(entity)

                                    val checkUser = personnelDao.getPersonnelById(entity.personnelId)
                                    Log.d(
                                        TAG,
                                        "本地同步成功：name=${checkUser?.name}, personnelId=${checkUser?.personnelId}, faceToken=${checkUser?.faceToken}"
                                    )
                                }
                            }
                        } else {
                            Log.e(TAG, "人员下发失败：JSON 中缺失核心主键 personnelId")
                        }
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