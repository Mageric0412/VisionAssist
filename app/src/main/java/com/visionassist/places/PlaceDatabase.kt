package com.visionassist.places

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for PlaceOfInterest entities
 */
@Database(
    entities = [PlaceOfInterest::class],
    version = 1,
    exportSchema = false
)
abstract class PlaceDatabase : RoomDatabase() {

    abstract fun placeOfInterestDao(): PlaceOfInterestDao

    companion object {
        private const val DATABASE_NAME = "visionassist_places.db"

        @Volatile
        private var INSTANCE: PlaceDatabase? = null

        fun getInstance(context: Context): PlaceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PlaceDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PlaceDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}