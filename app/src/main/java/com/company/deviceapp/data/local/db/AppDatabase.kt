package com.company.deviceapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PersonnelEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personnelDao(): PersonnelDao
}