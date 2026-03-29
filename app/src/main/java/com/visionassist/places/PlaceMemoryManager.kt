package com.visionassist.places

import android.content.Context
import timber.log.Timber

/**
 * Main manager for place-based memory features
 * Coordinates between PlaceRepository and GeofenceManager
 */
class PlaceMemoryManager(private val context: Context) {

    companion object {
        private const val TAG = "PlaceMemoryManager"
    }

    private val repository = PlaceRepository(context)
    private val geofenceManager = GeofenceManager(context)

    interface PlaceMemoryListener {
        fun onPlacesLoaded(places: List<PlaceOfInterest>)
        fun onPlaceAdded(place: PlaceOfInterest)
        fun onPlaceUpdated(place: PlaceOfInterest)
        fun onPlaceDeleted(place: PlaceOfInterest)
        fun onError(message: String)
    }

    var listener: PlaceMemoryListener? = null

    /**
     * Initialize geofences for all active places
     */
    fun initializeGeofences(onResult: (Boolean) -> Unit) {
        val activePlaces = repository.getActivePlaces()

        if (activePlaces.isEmpty()) {
            Timber.d("$TAG: No active places to geofence")
            onResult(true)
            return
        }

        geofenceManager.addGeofences(
            places = activePlaces,
            onSuccess = {
                Timber.d("$TAG: Geofences initialized for ${activePlaces.size} places")
                onResult(true)
            },
            onFailure = { error ->
                Timber.e("$TAG: Failed to initialize geofences: $error")
                onResult(false)
            }
        )
    }

    /**
     * Get all places
     */
    fun getAllPlaces(): List<PlaceOfInterest> = repository.getAllPlaces()

    /**
     * Get active places
     */
    fun getActivePlaces(): List<PlaceOfInterest> = repository.getActivePlaces()

    /**
     * Get place by ID
     */
    fun getPlaceById(id: Long): PlaceOfInterest? = repository.getPlaceById(id)

    /**
     * Add a new place and register its geofence
     */
    fun addPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 20f,
        reminderText: String,
        isActive: Boolean = true,
        onResult: (Long) -> Unit
    ) {
        // Check geofence limit
        if (repository.getActivePlaceCount() >= PlaceRepository.MAX_GEOFENCES) {
            Timber.w("$TAG: Geofence limit reached")
            listener?.onError("Geofence limit (${PlaceRepository.MAX_GEOFENCES}) reached. Please delete some places first.")
            onResult(-1L)
            return
        }

        val id = repository.addPlace(name, latitude, longitude, radiusMeters, reminderText)

        if (id > 0) {
            val place = repository.getPlaceById(id)

            if (place == null) {
                Timber.e("$TAG: Failed to retrieve newly added place with id=$id")
                listener?.onError("Failed to retrieve newly added place")
                onResult(-1L)
                return
            }

            // If active, add geofence immediately
            if (isActive) {
                geofenceManager.addGeofences(
                    places = listOf(place),
                    onSuccess = {
                        listener?.onPlaceAdded(place)
                        onResult(id)
                    },
                    onFailure = { error ->
                        Timber.e("$TAG: Failed to add geofence: $error")
                        listener?.onError(error)
                        onResult(id)
                    }
                )
            } else {
                listener?.onPlaceAdded(place)
                onResult(id)
            }
        } else {
            listener?.onError("Failed to add place")
            onResult(-1L)
        }
    }

    /**
     * Update a place and refresh its geofence
     */
    fun updatePlace(place: PlaceOfInterest, onResult: (Boolean) -> Unit) {
        val updated = repository.updatePlace(place)

        if (updated) {
            // Refresh geofences
            refreshGeofences()
            listener?.onPlaceUpdated(place)
        }

        onResult(updated)
    }

    /**
     * Delete a place and remove its geofence
     */
    fun deletePlace(place: PlaceOfInterest, onResult: (Boolean) -> Unit) {
        val deleted = repository.deletePlace(place)

        if (deleted) {
            // Remove geofence
            geofenceManager.removeGeofencesByIds(
                placeIds = listOf(place.id),
                onSuccess = {
                    listener?.onPlaceDeleted(place)
                    onResult(true)
                },
                onFailure = { error ->
                    Timber.e("$TAG: Failed to remove geofence: $error")
                    listener?.onPlaceDeleted(place)
                    onResult(true) // Still report success since DB delete worked
                }
            )
        } else {
            onResult(false)
        }
    }

    /**
     * Toggle place active state
     */
    fun setPlaceActive(id: Long, isActive: Boolean, onResult: (Boolean) -> Unit) {
        val success = repository.setPlaceActive(id, isActive)

        if (success) {
            refreshGeofences()
        }

        onResult(success)
    }

    /**
     * Refresh all geofences from database
     */
    fun refreshGeofences() {
        // Remove all existing geofences first
        geofenceManager.removeAllGeofences(
            onSuccess = {
                // Re-add all active places
                initializeGeofences { }
            },
            onFailure = { error ->
                Timber.e("$TAG: Failed to refresh geofences: $error")
            }
        )
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean = geofenceManager.hasLocationPermission()

    /**
     * Get place count
     */
    fun getPlaceCount(): Int = repository.getPlaceCount()

    /**
     * Get active place count
     */
    fun getActivePlaceCount(): Int = repository.getActivePlaceCount()

    /**
     * Get remaining geofence slots
     */
    fun getRemainingGeofenceSlots(): Int {
        return PlaceRepository.MAX_GEOFENCES - repository.getActivePlaceCount()
    }
}