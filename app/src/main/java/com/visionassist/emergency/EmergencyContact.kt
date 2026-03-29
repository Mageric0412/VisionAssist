package com.visionassist.emergency

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Emergency contact data class
 * Stores emergency contact information for fall detection alerts
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val phoneNumber: String,

    val isPrimary: Boolean = false
) {
    /**
     * Check if this is an emergency number (120, 110, 119)
     */
    fun isEmergencyNumber(): Boolean {
        return phoneNumber in listOf("120", "110", "119", "112")
    }
}
