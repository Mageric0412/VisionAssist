package com.visionassist.places

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

/**
 * Manages Android Geofencing API for location-based triggers
 */
class GeofenceManager(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_EXPIRATION_MS = -1L  // Never expires
        private const val LOITERING_DELAY_MS = 5000     // 5 seconds dwell time
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // Pending intent for geofence broadcasts
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Before Q, foreground location permission implies background
            fineLocation
        }

        return fineLocation && backgroundLocation
    }

    /**
     * Add geofences for a list of places
     */
    fun addGeofences(places: List<PlaceOfInterest>, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!hasLocationPermission()) {
            onFailure("Location permission not granted")
            return
        }

        if (places.isEmpty()) {
            onSuccess()
            return
        }

        val geofences = places.mapNotNull { place ->
            createGeofence(place)
        }

        if (geofences.isEmpty()) {
            onSuccess()
            return
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Timber.d("$TAG: Added ${geofences.size} geofences")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "$TAG: Failed to add geofences")
                    onFailure(e.message ?: "Unknown error")
                }
        } catch (e: SecurityException) {
            Timber.e(e, "$TAG: Security exception adding geofences")
            onFailure("Permission denied")
        }
    }

    /**
     * Remove all geofences
     */
    fun removeAllGeofences(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Timber.d("$TAG: Removed all geofences")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Timber.e(e, "$TAG: Failed to remove geofences")
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Remove geofences by place IDs
     */
    fun removeGeofencesByIds(placeIds: List<Long>, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val geofenceIds = placeIds.map { it.toString() }
        geofencingClient.removeGeofences(geofenceIds)
            .addOnSuccessListener {
                Timber.d("$TAG: Removed ${placeIds.size} geofences")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Timber.e(e, "$TAG: Failed to remove geofences")
                onFailure(e.message ?: "Unknown error")
            }
    }

    /**
     * Create a Geofence object from a PlaceOfInterest
     */
    private fun createGeofence(place: PlaceOfInterest): Geofence? {
        return try {
            Geofence.Builder()
                .setRequestId(place.id.toString())
                .setCircularRegion(
                    place.latitude,
                    place.longitude,
                    place.radiusMeters
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
                .setLoiteringDelay(LOITERING_DELAY_MS)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT or
                            Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .build()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to create geofence for place ${place.id}")
            null
        }
    }
}