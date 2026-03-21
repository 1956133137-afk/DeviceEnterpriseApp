package com.company.deviceapp.ui.inspection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CardLoginPreviewArea(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier.background(Color(0xFF102027))
    ) {
        CameraId4Preview(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(
                    Color.Black.copy(alpha = 0.55f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Text(
                text = "刷卡登录预览（摄像头 ID = 4）",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = onClose) {
                Text("关闭刷卡预览", fontSize = 22.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CameraId4Preview(
    modifier: Modifier = Modifier,
    cameraId: String = "4",
    onFrameCaptured: ((Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var cameraDevice: CameraDevice? = null
                    private var captureSession: CameraCaptureSession? = null
                    private val thread = HandlerThread("card-camera-id4-thread").apply { start() }
                    private val handler = Handler(thread.looper)

                    private fun closeAll() {
                        try {
                            captureSession?.close()
                        } catch (_: Exception) {
                        }
                        captureSession = null

                        try {
                            cameraDevice?.close()
                        } catch (_: Exception) {
                        }
                        cameraDevice = null

                        try {
                            thread.quitSafely()
                        } catch (_: Exception) {
                        }
                    }

                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        try {
                            if (cameraId == "4") {
                                val matrix = android.graphics.Matrix()
                                val w = width.toFloat()
                                val h = height.toFloat()
                                matrix.postRotate(-90f, w / 2f, h / 2f)
                                matrix.postScale(-(w / h), h / w, w / 2f, h / 2f)
                                setTransform(matrix)
                            }

                            if (!cameraManager.cameraIdList.contains(cameraId)) {
                                android.util.Log.e("CARD_LOGIN", "未找到 cameraId=$cameraId")
                                return
                            }

                            surface.setDefaultBufferSize(
                                width.coerceAtLeast(1280),
                                height.coerceAtLeast(720)
                            )

                            cameraManager.openCamera(
                                cameraId,
                                object : CameraDevice.StateCallback() {
                                    override fun onOpened(camera: CameraDevice) {
                                        cameraDevice = camera
                                        val previewSurface = Surface(surface)

                                        try {
                                            val requestBuilder = camera.createCaptureRequest(
                                                CameraDevice.TEMPLATE_PREVIEW
                                            ).apply {
                                                addTarget(previewSurface)
                                                set(
                                                    CaptureRequest.CONTROL_AF_MODE,
                                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                                )
                                            }

                                            camera.createCaptureSession(
                                                listOf(previewSurface),
                                                object : CameraCaptureSession.StateCallback() {
                                                    override fun onConfigured(session: CameraCaptureSession) {
                                                        captureSession = session
                                                        try {
                                                            session.setRepeatingRequest(
                                                                requestBuilder.build(),
                                                                null,
                                                                handler
                                                            )
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("CARD_LOGIN", "setRepeatingRequest 失败", e)
                                                        }
                                                    }

                                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                                        android.util.Log.e("CARD_LOGIN", "cameraId=4 预览会话配置失败")
                                                    }
                                                },
                                                handler
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("CARD_LOGIN", "创建 cameraId=4 预览失败", e)
                                        }
                                    }

                                    override fun onDisconnected(camera: CameraDevice) {
                                        closeAll()
                                    }

                                    override fun onError(camera: CameraDevice, error: Int) {
                                        android.util.Log.e("CARD_LOGIN", "cameraId=4 打开失败 error=$error")
                                        closeAll()
                                    }
                                },
                                handler
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("CARD_LOGIN", "打开 cameraId=4 异常", e)
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        closeAll()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        onFrameCaptured?.let { callback ->
                            this@apply.bitmap?.let { bitmap ->
                                callback(bitmap)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}
