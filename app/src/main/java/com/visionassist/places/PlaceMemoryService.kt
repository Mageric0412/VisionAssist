package com.visionassist.places

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.visionassist.R
import com.visionassist.VisionAssistApp
import com.visionassist.speech.SpeechManager
import com.visionassist.ui.MainActivity
import timber.log.Timber

/**
 * Foreground Service for handling place-based reminders
 * Runs when a geofence transition is triggered
 */
class PlaceMemoryService : Service() {

    companion object {
        private const val TAG = "PlaceMemoryService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "place_memory_channel"

        const val EXTRA_PLACE_ID = "place_id"
        const val EXTRA_ACTION = "action"
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var speechManager: SpeechManager

    override fun onCreate() {
        super.onCreate()
        placeRepository = PlaceRepository(this)
        speechManager = VisionAssistApp.getInstance().speechManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val placeId = intent?.getLongExtra(EXTRA_PLACE_ID, -1L) ?: -1L
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: return START_NOT_STICKY

        if (placeId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        val place = placeRepository.getPlaceById(placeId)
        if (place == null) {
            Timber.w("$TAG: Place not found: $placeId")
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            GeofenceBroadcastReceiver.GeofenceAction.ENTER.name,
            GeofenceBroadcastReceiver.GeofenceAction.DWELL.name -> {
                handleEnter(place)
            }
            GeofenceBroadcastReceiver.GeofenceAction.EXIT.name -> {
                handleExit(place)
            }
        }

        // Update last triggered time
        placeRepository.markTriggered(placeId)

        stopSelf()
        return START_NOT_STICKY
    }

    private fun handleEnter(place: PlaceOfInterest) {
        Timber.d("$TAG: Entering place: ${place.name}")

        // Show notification
        showNotification(place.name, place.reminderText)

        // Announce with speech
        speechManager.speak("进入${place.name}。${place.reminderText}")
    }

    private fun handleExit(place: PlaceOfInterest) {
        Timber.d("$TAG: Exiting place: ${place.name}")

        // Optional: announce exit
        if (place.isEmergencyPlace()) {
            speechManager.speak("离开${place.name}")
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Place Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Location-based reminders"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}