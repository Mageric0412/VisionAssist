package com.visionassist.fall

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import timber.log.Timber

/**
 * Fall detection using accelerometer and gyroscope
 *
 * Algorithm (Phase 1 - threshold based):
 * - Fall detected when: acceleration magnitude > FALL_THRESHOLD (2.5g)
 *   followed by acceleration magnitude < SETTLE_THRESHOLD (0.5g) within SETTLE_WINDOW_MS (500ms)
 *
 * False positive reduction:
 * - Combined with gyroscope to detect if person is standing/sitting after
 * - If gyroscope shows continued movement, likely not a fall
 */
class FallDetector(
    private val context: Context,
    private val listener: FallDetectionListener
) : SensorEventListener {

    interface FallDetectionListener {
        /**
         * Called when a fall is detected
         * @param confidence How confident we are (0.0 - 1.0)
         */
        fun onFallDetected(confidence: Float)

        /**
         * Called when acceleration values are being processed
         * Used for debugging/calibration
         */
        fun onAccelerationUpdate(magnitude: Float)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Detection state
    private var isMonitoring = false

    // Fall detection parameters
    companion object {
        // Acceleration thresholds (in g, where 1g = 9.81 m/s^2)
        const val FALL_THRESHOLD = 2.5f       // Impact detection
        const val SETTLE_THRESHOLD = 0.5f      // Post-fall settling
        const val SETTLE_WINDOW_MS = 500L      // Time window to detect settle

        // Normal walking has acceleration around 0.5-1.0g
        // Sitting down quickly can reach 1.5-2.0g
        // Actual fall typically exceeds 2.5g

        // Gyroscope thresholds (rad/s)
        const val GYRO_PEAK_THRESHOLD = 2.0f // Rapid rotation indicates throw/sit

        // History buffer for detecting fall pattern
        const val HISTORY_SIZE = 20
        private const val TAG = "FallDetector"
    }

    // Data buffers
    private data class AccelReading(val magnitude: Float, val timestamp: Long)
    private val accelHistory = mutableListOf<AccelReading>()

    // Gyroscope state
    private var gyroMagnitude = 0f

    // State machine
    private var state: DetectionState = DetectionState.IDLE
    private var fallStartTime = 0L

    enum class DetectionState {
        IDLE,           // Normal monitoring
        IMPACT_DETECTED, // High acceleration detected
        POTENTIAL_FALL,  // Settling detected after impact
        FALL_CONFIRMED   // Fall confirmed
    }

    /**
     * Start monitoring for falls
     */
    fun startMonitoring() {
        if (isMonitoring) return

        accelerometer?.let { accel ->
            sensorManager.registerListener(
                this,
                accel,
                SensorManager.SENSOR_DELAY_GAME // ~50Hz
            )
        }

        gyroscope?.let { gyro ->
            sensorManager.registerListener(
                this,
                gyro,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        isMonitoring = true
        state = DetectionState.IDLE
        accelHistory.clear()
        Timber.d("$TAG: Started monitoring")
    }

    /**
     * Stop monitoring for falls
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        sensorManager.unregisterListener(this)
        isMonitoring = false
        state = DetectionState.IDLE
        accelHistory.clear()
        Timber.d("$TAG: Stopped monitoring")
    }

    /**
     * Check if currently monitoring
     */
    fun isMonitoring(): Boolean = isMonitoring

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> processGyroscope(event)
        }
    }

    private fun processAccelerometer(event: SensorEvent) {
        // Calculate magnitude (excluding gravity would be more accurate,
        // but for simplicity we use total magnitude)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        listener.onAccelerationUpdate(magnitude)

        // Add to history
        val now = System.currentTimeMillis()
        accelHistory.add(AccelReading(magnitude, now))

        // Keep only recent history
        while (accelHistory.size > HISTORY_SIZE) {
            accelHistory.removeAt(0)
        }

        // State machine for fall detection
        when (state) {
            DetectionState.IDLE -> {
                if (magnitude > FALL_THRESHOLD) {
                    // High impact detected
                    state = DetectionState.IMPACT_DETECTED
                    fallStartTime = now
                    Timber.d("$TAG: Impact detected: ${"%.2f".format(magnitude)}g")
                }
            }

            DetectionState.IMPACT_DETECTED -> {
                val elapsed = now - fallStartTime
                if (magnitude < SETTLE_THRESHOLD) {
                    // Settling detected within time window
                    if (elapsed < SETTLE_WINDOW_MS) {
                        // Check gyroscope to rule out false positives
                        if (gyroMagnitude < GYRO_PEAK_THRESHOLD) {
                            // Likely a fall
                            state = DetectionState.FALL_CONFIRMED
                            Timber.d("$TAG: Fall confirmed, settling at ${"%.2f".format(magnitude)}g")
                            listener.onFallDetected(calculateConfidence())
                            state = DetectionState.IDLE // Reset after detection
                        } else {
                            // High rotation - likely sitting down or throwing phone
                            Timber.d("$TAG: Rejected - high gyroscope ${"%.2f".format(gyroMagnitude)}")
                            state = DetectionState.IDLE
                        }
                    } else {
                        // Took too long to settle
                        Timber.d("$TAG: Rejected - took too long to settle (${elapsed}ms)")
                        state = DetectionState.IDLE
                    }
                } else if (elapsed > SETTLE_WINDOW_MS) {
                    // No settling within window
                    Timber.d("$TAG: Rejected - no settling within window")
                    state = DetectionState.IDLE
                }
            }

            DetectionState.POTENTIAL_FALL,
            DetectionState.FALL_CONFIRMED -> {
                // Should not happen, but reset if it does
                state = DetectionState.IDLE
            }
        }
    }

    private fun processGyroscope(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        gyroMagnitude = sqrt(x * x + y * y + z * z)
    }

    /**
     * Calculate confidence of the fall detection
     * Based on how close to thresholds and timing
     */
    private fun calculateConfidence(): Float {
        if (accelHistory.size < 2) return 0.5f

        val impactReading = accelHistory.lastOrNull { it.magnitude > FALL_THRESHOLD }
        val settleReading = accelHistory.lastOrNull { it.magnitude < SETTLE_THRESHOLD }

        if (impactReading == null || settleReading == null) return 0.5f

        // Higher impact magnitude = higher confidence
        val impactScore = (impactReading.magnitude / FALL_THRESHOLD).coerceIn(0f, 2f) / 2f

        // Faster settle = higher confidence
        val settleTime = settleReading.timestamp - impactReading.timestamp
        val timeScore = (1f - (settleTime.toFloat() / SETTLE_WINDOW_MS)).coerceIn(0f, 1f)

        // Lower gyroscope = higher confidence
        val gyroScore = (1f - (gyroMagnitude / GYRO_PEAK_THRESHOLD)).coerceIn(0f, 1f)

        // Combined score (weighted)
        return (impactScore * 0.4f + timeScore * 0.4f + gyroScore * 0.2f).coerceIn(0.3f, 0.95f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for our detection algorithm
    }
}
