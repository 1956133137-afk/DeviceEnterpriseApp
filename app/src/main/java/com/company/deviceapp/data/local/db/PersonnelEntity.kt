package com.company.deviceapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// 严格对应 MQTT 协议中的人员字段
@Entity(tableName = "personnel")
data class PersonnelEntity(
    @PrimaryKey val personnelId: String,
    val name: String?,
    val faceFeatureImgPath: String?, // 用于本地人脸特征提取比对
    val icNum: String?,
    val jobNum: String?,
    val memberType: String?,
    val personnelType: String?,
    val phone: String?,
    val healthCard: String?,         // 健康证照片URL
    val healthCardTime: String?      // 健康证有效期
)

@Dao
interface PersonnelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePersonnel(personnel: PersonnelEntity)

    @Query("DELETE FROM personnel WHERE personnelId = :id")
    suspend fun deletePersonnelById(id: String)

    @Query("DELETE FROM personnel")
    suspend fun deleteAllPersonnel()

    @Query("SELECT * FROM personnel WHERE personnelId = :id")
    suspend fun getPersonnelById(id: String): PersonnelEntity?
}