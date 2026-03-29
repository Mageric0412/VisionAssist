package com.visionassist.places

import android.content.Context
import timber.log.Timber

/**
 * Repository for managing PlaceOfInterest data
 * Provides CRUD operations and geofence management
 */
class PlaceRepository(context: Context) {

    companion object {
        private const val TAG = "PlaceRepository"
        // Android Geofencing API limit
        const val MAX_GEOFENCES = 100
    }

    private val database = PlaceDatabase.getInstance(context)
    private val dao = database.placeOfInterestDao()

    /**
     * Get all places
     */
    fun getAllPlaces(): List<PlaceOfInterest> {
        return try {
            dao.getAllPlaces()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all places")
            emptyList()
        }
    }

    /**
     * Get all active places
     */
    fun getActivePlaces(): List<PlaceOfInterest> {
        return try {
            dao.getActivePlaces()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get active places")
            emptyList()
        }
    }

    /**
     * Get place by ID
     */
    fun getPlaceById(id: Long): PlaceOfInterest? {
        return try {
            dao.getPlaceById(id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get place by id: $id")
            null
        }
    }

    /**
     * Add a new place
     */
    fun addPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 20f,
        reminderText: String
    ): Long {
        return try {
            val place = PlaceOfInterest(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                reminderText = reminderText
            )
            val id = dao.insert(place)
            Timber.d("Added place: $name with id=$id")
            id
        } catch (e: Exception) {
            Timber.e(e, "Failed to add place: $name")
            -1
        }
    }

    /**
     * Update a place
     */
    fun updatePlace(place: PlaceOfInterest): Boolean {
        return try {
            dao.update(place)
            Timber.d("Updated place: ${place.name}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update place: ${place.name}")
            false
        }
    }

    /**
     * Delete a place
     */
    fun deletePlace(place: PlaceOfInterest): Boolean {
        return try {
            dao.delete(place)
            Timber.d("Deleted place: ${place.name}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete place: ${place.name}")
            false
        }
    }

    /**
     * Delete a place by ID
     */
    fun deletePlaceById(id: Long): Boolean {
        return try {
            dao.deleteById(id)
            Timber.d("Deleted place id=$id")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete place id=$id")
            false
        }
    }

    /**
     * Mark a place as triggered
     */
    fun markTriggered(id: Long) {
        try {
            dao.updateLastTriggered(id, System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark triggered: $id")
        }
    }

    /**
     * Toggle place active state
     */
    fun setPlaceActive(id: Long, isActive: Boolean): Boolean {
        return try {
            dao.setActive(id, isActive)
            Timber.d("Set place $id active=$isActive")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to set active: $id")
            false
        }
    }

    /**
     * Get total place count
     */
    fun getPlaceCount(): Int {
        return try {
            dao.getPlaceCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get place count")
            0
        }
    }

    /**
     * Get active place count
     */
    fun getActivePlaceCount(): Int {
        return try {
            dao.getActivePlaceCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get active place count")
            0
        }
    }

    /**
     * Check if we can add more places (below geofence limit)
     */
    fun canAddPlace(): Boolean {
        return getActivePlaceCount() < MAX_GEOFENCES
    }
}