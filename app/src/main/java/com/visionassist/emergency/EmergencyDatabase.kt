package com.visionassist.emergency

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for emergency contacts
 */
@Database(
    entities = [EmergencyContact::class],
    version = 1,
    exportSchema = false
)
abstract class EmergencyDatabase : RoomDatabase() {

    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: EmergencyDatabase? = null

        fun getInstance(context: Context): EmergencyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EmergencyDatabase::class.java,
                    "visionassist_emergency.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
