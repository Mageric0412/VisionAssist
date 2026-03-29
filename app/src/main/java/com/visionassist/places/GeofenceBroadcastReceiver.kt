package com.visionassist.places

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

/**
 * BroadcastReceiver for handling geofence transition events
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Timber.e("$TAG: GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Timber.e("$TAG: Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: run {
            Timber.w("$TAG: No triggering geofences")
            return
        }

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Timber.d("$TAG: Geofence ENTER")
                handleGeofenceEnter(context, triggeringGeofences)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Timber.d("$TAG: Geofence EXIT")
                handleGeofenceExit(context, triggeringGeofences)
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Timber.d("$TAG: Geofence DWELL")
                handleGeofenceDwell(context, triggeringGeofences)
            }
            else -> {
                Timber.w("$TAG: Unknown transition type: $transitionType")
            }
        }
    }

    private fun handleGeofenceEnter(context: Context, geofences: List<Geofence>) {
        for (geofence in geofences) {
            val placeId = geofence.requestId
            Timber.d("$TAG: Entered place: $placeId")
            // Notify the service to handle this
            notifyPlaceService(context, placeId.toLong(), GeofenceAction.ENTER)
        }
    }

    private fun handleGeofenceExit(context: Context, geofences: List<Geofence>) {
        for (geofence in geofences) {
            val placeId = geofence.requestId
            Timber.d("$TAG: Exited place: $placeId")
            notifyPlaceService(context, placeId.toLong(), GeofenceAction.EXIT)
        }
    }

    private fun handleGeofenceDwell(context: Context, geofences: List<Geofence>) {
        for (geofence in geofences) {
            val placeId = geofence.requestId
            Timber.d("$TAG: Dwelt in place: $placeId")
            // DWELL is similar to ENTER but indicates user stayed for a while
            notifyPlaceService(context, placeId.toLong(), GeofenceAction.DWELL)
        }
    }

    private fun notifyPlaceService(context: Context, placeId: Long, action: GeofenceAction) {
        // Send broadcast to PlaceMemoryService to handle the reminder
        val serviceIntent = Intent(context, PlaceMemoryService::class.java).apply {
            putExtra(PlaceMemoryService.EXTRA_PLACE_ID, placeId)
            putExtra(PlaceMemoryService.EXTRA_ACTION, action.name)
        }
        context.startService(serviceIntent)
    }

    enum class GeofenceAction {
        ENTER, EXIT, DWELL
    }
}