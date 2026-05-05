package com.visionassist.navigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber

/**
 * Compass manager using accelerometer and magnetometer for heading direction
 */
class CompassManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "CompassManager"
        // Smoothing factor for low-pass filter (0-1, lower = more smoothing)
        private const val ALPHA = 0.15f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    interface CompassListener {
        fun onHeadingChanged(heading: Float)
    }

    var listener: CompassListener? = null

    // Sensor data
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Filtered heading
    private var currentHeading = 0f

    // Low-pass filter
    private fun lowPassFilter(input: Float, previousOutput: Float): Float {
        return previousOutput + ALPHA * (input - previousOutput)
    }

    /**
     * Start compass updates
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        Timber.d("$TAG: Started")
    }

    /**
     * Stop compass updates
     */
    fun stop() {
        sensorManager.unregisterListener(this)
        Timber.d("$TAG: Stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        // Calculate orientation
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert azimuth to degrees
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Normalize to 0-360
            val normalizedHeading = if (azimuth < 0) azimuth + 360 else azimuth

            // Apply low-pass filter for smoothing
            currentHeading = lowPassFilter(normalizedHeading, currentHeading)

            listener?.onHeadingChanged(currentHeading)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                    Timber.w("$TAG: Compass accuracy unreliable")
                }
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                    Timber.d("$TAG: Compass accuracy low")
                }
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                    Timber.d("$TAG: Compass accuracy medium")
                }
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                    Timber.d("$TAG: Compass accuracy high")
                }
            }
        }
    }

    /**
     * Get current heading in degrees (0 = North, 90 = East, 180 = South, 270 = West)
     */
    fun getHeading(): Float = currentHeading

    /**
     * Get cardinal direction name
     */
    fun getCardinalDirection(): String {
        return when {
            currentHeading >= 337.5 || currentHeading < 22.5 -> "北"
            currentHeading >= 22.5 && currentHeading < 67.5 -> "东北"
            currentHeading >= 67.5 && currentHeading < 112.5 -> "东"
            currentHeading >= 112.5 && currentHeading < 157.5 -> "东南"
            currentHeading >= 157.5 && currentHeading < 202.5 -> "南"
            currentHeading >= 202.5 && currentHeading < 247.5 -> "西南"
            currentHeading >= 247.5 && currentHeading < 292.5 -> "西"
            currentHeading >= 292.5 && currentHeading < 337.5 -> "西北"
            else -> "未知"
        }
    }
}