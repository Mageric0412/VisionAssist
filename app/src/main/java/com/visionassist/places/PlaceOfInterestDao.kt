package com.visionassist.places

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for PlaceOfInterest entities
 */
@Dao
interface PlaceOfInterestDao {

    @Query("SELECT * FROM places_of_interest ORDER BY createdAt DESC")
    fun getAllPlaces(): List<PlaceOfInterest>

    @Query("SELECT * FROM places_of_interest WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActivePlaces(): List<PlaceOfInterest>

    @Query("SELECT * FROM places_of_interest WHERE id = :id")
    fun getPlaceById(id: Long): PlaceOfInterest?

    @Query("SELECT * FROM places_of_interest WHERE name LIKE '%' || :query || '%'")
    fun searchPlaces(query: String): List<PlaceOfInterest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(place: PlaceOfInterest): Long

    @Update
    fun update(place: PlaceOfInterest)

    @Delete
    fun delete(place: PlaceOfInterest)

    @Query("DELETE FROM places_of_interest WHERE id = :id")
    fun deleteById(id: Long)

    @Query("UPDATE places_of_interest SET lastTriggered = :timestamp WHERE id = :id")
    fun updateLastTriggered(id: Long, timestamp: Long)

    @Query("UPDATE places_of_interest SET isActive = :isActive WHERE id = :id")
    fun setActive(id: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM places_of_interest")
    fun getPlaceCount(): Int

    @Query("SELECT COUNT(*) FROM places_of_interest WHERE isActive = 1")
    fun getActivePlaceCount(): Int
}