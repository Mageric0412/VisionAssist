package com.visionassist.navigation

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.visionassist.VisionAssistApp
import com.visionassist.detector.DetectedObject
import com.visionassist.speech.SpeechManager
import timber.log.Timber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Navigation manager that coordinates GPS, step counting, and obstacle detection
 * to provide advance warnings when obstacles are in the user's path.
 *
 * Data flow:
 * GPS/Compass ──▶ NavigationManager ──▶ SpeechManager ──▶ User
 *                          ▲
 *                          │
 *               ObjectDetector (obstacles in path)
 */
class NavigationManager(
    private val context: Context,
    private val speechManager: SpeechManager
) {
    companion object {
        private const val TAG = "NavigationManager"

        // Distance thresholds
        const val WARNING_DISTANCE_NEAR = 3f    // meters - announce immediately
        const val WARNING_DISTANCE_FAR = 8f     // meters - announce if moving toward

        // Minimum confidence for obstacle detection to be considered
        const val MIN_OBSTACLE_CONFIDENCE = 0.5f

        // Location update interval (ms)
        const val LOCATION_UPDATE_INTERVAL = 1000L
        const val LOCATION_FASTEST_INTERVAL = 500L

        // Navigation state
        const val DIRECTION_AHEAD = 0
        const val DIRECTION_LEFT = 1
        const val DIRECTION_RIGHT = 2
    }

    interface NavigationListener {
        fun onLocationChanged(location: Location)
        fun onDirectionChanged(direction: Int, targetDistance: Float?)
        fun onObstacleWarning(obstacle: DetectedObject, distance: Float, direction: Int)
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var currentLocation: Location? = null
    private var currentHeading: Float = 0f  // compass heading in degrees
    private var isNavigating = false
    var listener: NavigationListener? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation = location
                listener?.onLocationChanged(location)
                Timber.v("$TAG: Location update: ${location.latitude}, ${location.longitude}")
            }
        }
    }

    /**
     * Start navigation mode
     */
    fun startNavigation(listener: NavigationListener? = null) {
        if (isNavigating) return

        this.listener = listener
        isNavigating = true

        // Request location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Timber.d("$TAG: Started navigation")
        } catch (e: SecurityException) {
            Timber.e(e, "$TAG: Location permission not granted")
            speechManager.speak("导航需要位置权限，请在设置中开启")
            isNavigating = false
        }
    }

    /**
     * Stop navigation mode
     */
    fun stopNavigation() {
        if (!isNavigating) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isNavigating = false
        listener = null
        Timber.d("$TAG: Stopped navigation")
    }

    /**
     * Check if currently navigating
     */
    fun isNavigating(): Boolean = isNavigating

    /**
     * Update heading from compass sensor
     */
    fun updateHeading(heading: Float) {
        currentHeading = heading
    }

    /**
     * Process detected object and check if it's in the navigation path
     * Called from MainActivity when objects are detected
     */
    fun processDetectedObject(obj: DetectedObject) {
        if (!isNavigating || currentLocation == null) return

        // Only process high-confidence detections
        if (obj.confidence < MIN_OBSTACLE_CONFIDENCE) return

        // Calculate distance based on object's estimated distance
        val obstacleDistance = obj.estimatedDistance ?: return

        // Determine direction relative to current heading
        val direction = calculateObstacleDirection(obj)

        // Check if obstacle is in our path (ahead or slightly to sides)
        val isInPath = when (direction) {
            DIRECTION_AHEAD -> true
            DIRECTION_LEFT, DIRECTION_RIGHT -> obstacleDistance < WARNING_DISTANCE_FAR
            else -> false
        }

        if (isInPath && obstacleDistance < WARNING_DISTANCE_FAR) {
            val warningDirection = when (direction) {
                DIRECTION_AHEAD -> "正前方"
                DIRECTION_LEFT -> "左侧"
                DIRECTION_RIGHT -> "右侧"
                else -> "后方"
            }

            val announcement = buildObstacleWarning(obj, obstacleDistance, warningDirection)
            speechManager.speak(announcement)
            listener?.onObstacleWarning(obj, obstacleDistance, direction)

            Timber.d("$TAG: Obstacle warning - $warningDirection ${"%.1f".format(obstacleDistance)}米")
        }
    }

    /**
     * Calculate obstacle direction relative to current heading
     * Uses the object's frame position to estimate angle
     *
     * Note: centerX is in pixel coordinates from the detector (not normalized).
     * We use the object's getRelativeX() method which properly handles frame width.
     */
    private fun calculateObstacleDirection(obj: DetectedObject): Int {
        // Use getRelativeX which properly divides by actual frame width
        // This handles different frame sizes correctly
        val frameWidth = 640f  // Standard CameraX preview width
        val normalizedX = obj.getRelativeX(frameWidth.toInt())

        // Determine direction based on position
        // Left side of frame = right side of world (if heading forward)
        return when {
            normalizedX < 0.35f -> DIRECTION_RIGHT  // Object on left of frame = obstacle on right
            normalizedX > 0.65f -> DIRECTION_LEFT   // Object on right of frame = obstacle on left
            else -> DIRECTION_AHEAD                  // Center = directly ahead
        }
    }

    /**
     * Build obstacle warning announcement
     */
    private fun buildObstacleWarning(obj: DetectedObject, distance: Float, direction: String): String {
        val label = obj.label
        val distanceText = when {
            distance < 1f -> "不到1米"
            distance < 2f -> "约1米"
            distance < 3f -> "约2米"
            distance < 5f -> "约${distance.toInt()}米"
            else -> "约${distance.toInt()}米"
        }
        return "$direction${distanceText}有$label"
    }

    /**
     * Get current location
     */
    fun getCurrentLocation(): Location? = currentLocation

    /**
     * Get current heading
     */
    fun getCurrentHeading(): Float = currentHeading
}