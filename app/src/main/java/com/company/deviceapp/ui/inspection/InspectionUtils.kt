package com.company.deviceapp.ui.inspection

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun copyRecognizedFaceImage(context: Context, sourcePath: String): String? {
    return try {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            android.util.Log.e("FACE_UI", "源图片不存在或为空: $sourcePath")
            return null
        }

        val targetDir = File(context.filesDir, "recognized_faces")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = File(targetDir, "face_${System.currentTimeMillis()}.jpg")

        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        android.util.Log.d(
            "FACE_UI",
            "抓拍照片复制成功: ${targetFile.absolutePath}, size=${targetFile.length()}"
        )
        targetFile.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("FACE_UI", "复制抓拍照片失败: $sourcePath", e)
        null
    }
}
