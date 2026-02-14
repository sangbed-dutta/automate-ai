package com.devfest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlowEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun flowDao(): FlowDao

    companion object {
        fun build(context: Context): AutomationDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AutomationDatabase::class.java,
                "automation.db"
            ).fallbackToDestructiveMigration().build()
    }
}
