package com.company.deviceapp.ui.inspection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun InspectionRecordInfoCard(
    faceImgPath: String?,
    handImg1Path: String?,
    handImg2Path: String?,
    memberUserId: String,
    inspectionTime: String,
    temperature: String,
    identifyType: String,
    openDoor: String,
    handType: String,
    healthCertificate: String,
    tempType: String,
    status: String,
    username: String,
    inspectionDesc: String,
    onDeleteFaceImg: () -> Unit = {},
    onRetakeFaceImg: () -> Unit = {},
    onDeleteHandImg1: () -> Unit = {},
    onRetakeHandImg1: () -> Unit = {},
    onDeleteHandImg2: () -> Unit = {},
    onRetakeHandImg2: () -> Unit = {},
    onOpenDoorStatusChange: (String) -> Unit = {},
    onConfirmSubmit: () -> Unit = {},
    isPhotosComplete: Boolean = false,
    canSubmit: Boolean = true,
    onShowQuestionnaire: () -> Unit = {}
) {
    val faceBitmap by produceState<Bitmap?>(initialValue = null, faceImgPath) {
        value = null
        val path = faceImgPath
        if (path.isNullOrBlank()) return@produceState
        repeat(12) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) { value = bitmap; return@produceState }
            }
            delay(200)
        }
    }
    val handBitmap1 by produceState<Bitmap?>(initialValue = null, handImg1Path) {
        value = null
        val path = handImg1Path
        if (path.isNullOrBlank()) return@produceState
        repeat(8) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) { value = bitmap; return@produceState }
            }
            delay(150)
        }
    }
    val handBitmap2 by produceState<Bitmap?>(initialValue = null, handImg2Path) {
        value = null
        val path = handImg2Path
        if (path.isNullOrBlank()) return@produceState
        repeat(8) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) { value = bitmap; return@produceState }
            }
            delay(150)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1565C0))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "晨检详情预览",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF263238)
                    )

                    Spacer(Modifier.width(32.dp))
                    Text(
                        text = "问卷调查 >",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.clickable { onShowQuestionnaire() }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                PhotoFieldItem("人脸识别", faceBitmap, onDeleteFaceImg, onRetakeFaceImg, Modifier.weight(2f))
                PhotoFieldItem("手掌检测", handBitmap1, onDeleteHandImg1, onRetakeHandImg1, Modifier.weight(2f))
                PhotoFieldItem("手背检测", handBitmap2, onDeleteHandImg2, onRetakeHandImg2, Modifier.weight(2f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("受检人员", fontSize = 24.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Text(username, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("工号ID", fontSize = 24.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Text(memberUserId, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
                
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前体温", fontSize = 24.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${temperature}°C",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = if (tempType == "1") Color(0xFF2E7D32) else Color(0xFFD84315)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("健康证状态", fontSize = 24.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniStatusBadge(if (healthCertificate == "1") "正常" else "已过期", healthCertificate == "1")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("闸机权限控制", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
                    Spacer(Modifier.width(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatusChip("允许进入", isSelected = openDoor == "1", activeColor = Color(0xFF2E7D32)) { onOpenDoorStatusChange("1") }
                        ModernStatusChip("禁止通过", isSelected = openDoor == "0", activeColor = Color(0xFF455A64)) { onOpenDoorStatusChange("0") }
                    }
                }
                
                Button(
                    onClick = onConfirmSubmit,
                    modifier = Modifier
                        .width(220.dp) 
                        .height(80.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPhotosComplete) Color(0xFF00796B) else Color(0xFF9E9E9E)
                    ),
                    enabled = canSubmit
                ) {
                    Text("确认提交", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, isSuccess: Boolean) {
    Surface(
        color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MiniStatusBadge(text: String, isSuccess: Boolean) {
    Surface(
        color = if (isSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, if (isSuccess) Color(0xFFBBF7D0) else Color(0xFFFECACA))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B),
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ModernStatusChip(text: String, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) activeColor else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
            fontSize = 20.sp,
            color = if (isSelected) Color.White else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhotoFieldItem(title: String, bitmap: Bitmap?, onDelete: () -> Unit, onRetake: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clickable(enabled = bitmap == null) { onRetake() },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AnimatedContent(
                    targetState = bitmap,
                    transitionSpec = { fadeIn() with fadeOut() }
                ) { targetBitmap ->
                    if (targetBitmap != null) {
                        Image(
                            bitmap = targetBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
            
            if (bitmap != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRetake, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) { 
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            } else {
                Surface(
                    onClick = onRetake,
                    shape = CircleShape,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.size(64.dp).shadow(8.dp, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}
