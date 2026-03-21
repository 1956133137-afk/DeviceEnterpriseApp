package com.company.deviceapp.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "personnel")
data class PersonnelEntity(
    @PrimaryKey val personnelId: String,
    val name: String?,
    val faceFeatureImgPath: String?,
    val faceToken: String?,
    val icNum: String?,
    val jobNum: String?,
    val memberType: String?,
    val personnelType: String?,
    val phone: String?,
    val healthCard: String?,
    val healthCardTime: String?
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

    @Query("SELECT * FROM personnel WHERE faceToken = :faceToken LIMIT 1")
    suspend fun getPersonnelByFaceToken(faceToken: String): PersonnelEntity?

    @Query("SELECT * FROM personnel WHERE icNum = :icNum LIMIT 1")
    suspend fun getPersonnelByIcNum(icNum: String): PersonnelEntity?
}