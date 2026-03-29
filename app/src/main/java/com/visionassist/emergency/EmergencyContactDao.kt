package com.visionassist.emergency

import androidx.room.*

/**
 * Data Access Object for EmergencyContact
 */
@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, id ASC")
    fun getAllContacts(): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryContact(): EmergencyContact?

    @Query("SELECT * FROM emergency_contacts WHERE id = :id")
    fun getContactById(id: Long): EmergencyContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: EmergencyContact): Long

    @Update
    fun update(contact: EmergencyContact)

    @Delete
    fun delete(contact: EmergencyContact)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM emergency_contacts")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    fun getContactCount(): Int
}
