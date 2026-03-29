package com.visionassist.fall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.visionassist.R
import com.visionassist.VisionAssistApp
import com.visionassist.ui.FallAlertActivity
import timber.log.Timber

/**
 * Foreground Service for fall detection
 *
 * Runs in background to monitor accelerometer for fall detection
 * Shows a persistent notification while monitoring is active
 */
class FallDetectionService : Service(), FallDetector.FallDetectionListener {

    companion object {
        private const val TAG = "FallDetectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fall_detection_channel"

        const val ACTION_START = "com.visionassist.action.START_FALL_DETECTION"
        const val ACTION_STOP = "com.visionassist.action.STOP_FALL_DETECTION"
    }

    private val binder = LocalBinder()
    private var fallDetector: FallDetector? = null
    private var isRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): FallDetectionService = this@FallDetectionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("$TAG: Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startDetection()
            ACTION_STOP -> stopDetection()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun startDetection() {
        if (isRunning) {
            Timber.w("$TAG: Already running")
            return
        }

        // Start foreground service with notification
        val notification = buildNotification(isActive = true)
        startForeground(NOTIFICATION_ID, notification)

        // Initialize and start fall detector
        fallDetector = FallDetector(applicationContext, this)
        fallDetector?.startMonitoring()

        isRunning = true
        Timber.d("$TAG: Detection started")
    }

    private fun stopDetection() {
        if (!isRunning) {
            Timber.w("$TAG: Not running")
            return
        }

        fallDetector?.stopMonitoring()
        fallDetector = null
        isRunning = false

        // Update notification to show inactive
        val notification = buildNotification(isActive = false)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Timber.d("$TAG: Detection stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fall Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when fall detection is active"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isActive: Boolean): Notification {
        val contentIntent = Intent(this, VisionAssistApp.getInstance().javaClass)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FallDetectionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isActive) "Fall Detection Active" else "Fall Detection"
        val text = if (isActive) "Monitoring for falls..." else "Tap to open"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(isActive)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // FallDetector.FallDetectionListener implementation

    override fun onFallDetected(confidence: Float) {
        Timber.d("$TAG: Fall detected with confidence $confidence")

        // Stop detection temporarily to prevent multiple triggers
        fallDetector?.stopMonitoring()

        // Launch FallAlertActivity
        val intent = Intent(this, FallAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)

        // Restart detection after a delay (to avoid rapid re-triggering)
        android.os.Handler(mainLooper).postDelayed({
            if (isRunning) {
                fallDetector?.startMonitoring()
            }
        }, 10000) // 10 second cooldown
    }

    override fun onAccelerationUpdate(magnitude: Float) {
        // Debugging/calibration - could log or update UI
        Timber.v("$TAG: Accel magnitude: ${"%.2f".format(magnitude)}")
    }

    override fun onDestroy() {
        super.onDestroy()
        fallDetector?.stopMonitoring()
        fallDetector = null
        isRunning = false
        Timber.d("$TAG: Destroyed")
    }

    /**
     * Check if fall detection is currently running
     */
    fun isDetectionActive(): Boolean = isRunning
}
