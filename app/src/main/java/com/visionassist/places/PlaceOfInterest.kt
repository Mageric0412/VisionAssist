package com.visionassist.places

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Place of Interest (POI) for location-based reminders
 * Stored in Room database
 */
@Entity(tableName = "places_of_interest")
data class PlaceOfInterest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,                    // User-defined name
    val latitude: Double,                // GPS latitude
    val longitude: Double,              // GPS longitude
    val radiusMeters: Float = 20f,       // Trigger radius (default 20m)
    val reminderText: String,            // Custom reminder message
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null,     // Last time geofence was triggered
    val isActive: Boolean = true          // Whether geofence is active
) {
    /**
     * Check if this is an emergency place (like home, work)
     */
    fun isEmergencyPlace(): Boolean {
        val emergencyKeywords = listOf("家", "home", "工作", "work", "父母", "parents")
        return emergencyKeywords.any { name.contains(it, ignoreCase = true) }
    }
}