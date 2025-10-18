package com.sid.smartmoisture.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Equation::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equationDao(): EquationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "SmartMoisture.db"
            ).fallbackToDestructiveMigration(false).build().also { INSTANCE = it }
        }
    }
}