package com.visionassist.config

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class AppConfig(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Detection Engine: "google_mlkit", "huawei_hms", "tflite_yolo"
    var detectionEngine: String
        get() = prefs.getString(KEY_DETECTION_ENGINE, ENGINE_GOOGLE_ML) ?: ENGINE_GOOGLE_ML
        set(value) = prefs.edit().putString(KEY_DETECTION_ENGINE, value).apply()

    // Speech Engine: "android_tts", "huawei_hms"
    var speechEngine: String
        get() = prefs.getString(KEY_SPEECH_ENGINE, ENGINE_ANDROID_TTS) ?: ENGINE_ANDROID_TTS
        set(value) = prefs.edit().putString(KEY_SPEECH_ENGINE, value).apply()

    // Detection sensitivity (0.0 - 1.0)
    var detectionSensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value.coerceIn(0f, 1f)).apply()

    // Report distance
    var reportDistance: Boolean
        get() = prefs.getBoolean(KEY_REPORT_DISTANCE, true)
        set(value) = prefs.edit().putBoolean(KEY_REPORT_DISTANCE, value).apply()

    // Report direction
    var reportDirection: Boolean
        get() = prefs.getBoolean(KEY_REPORT_DIRECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_REPORT_DIRECTION, value).apply()

    // Report size
    var reportSize: Boolean
        get() = prefs.getBoolean(KEY_REPORT_SIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_REPORT_SIZE, value).apply()

    // Report scene
    var reportScene: Boolean
        get() = prefs.getBoolean(KEY_REPORT_SCENE, true)
        set(value) = prefs.edit().putBoolean(KEY_REPORT_SCENE, value).apply()

    // Speech rate (0.5 - 2.0)
    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value.coerceIn(0.5f, 2.0f)).apply()

    // Speech pitch (0.5 - 2.0)
    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value.coerceIn(0.5f, 2.0f)).apply()

    // Minimum detection confidence (0.0 - 1.0)
    var minConfidence: Float
        get() = prefs.getFloat(KEY_MIN_CONFIDENCE, 0.6f)
        set(value) = prefs.edit().putFloat(KEY_MIN_CONFIDENCE, value.coerceIn(0f, 1f)).apply()

    // Report interval in milliseconds
    var reportIntervalMs: Long
        get() = prefs.getLong(KEY_REPORT_INTERVAL, 1500L)
        set(value) = prefs.edit().putLong(KEY_REPORT_INTERVAL, value.coerceAtLeast(500L)).apply()

    // Enable vibration feedback
    var vibrationFeedback: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_FEEDBACK, value).apply()

    // Camera facing: "back", "front"
    var cameraFacing: String
        get() = prefs.getString(KEY_CAMERA_FACING, CAMERA_BACK) ?: CAMERA_BACK
        set(value) = prefs.edit().putString(KEY_CAMERA_FACING, value).apply()

    // Detection distance threshold (meters)
    var distanceThreshold: Float
        get() = prefs.getFloat(KEY_DISTANCE_THRESHOLD, 3.0f)
        set(value) = prefs.edit().putFloat(KEY_DISTANCE_THRESHOLD, value.coerceIn(0.5f, 10f)).apply()

    // Focus on closest object only
    var focusClosestOnly: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_CLOSEST, true)
        set(value) = prefs.edit().putBoolean(KEY_FOCUS_CLOSEST, value).apply()

    // Object categories to detect (comma-separated label indices)
    var enabledCategories: String
        get() = prefs.getString(KEY_ENABLED_CATEGORIES, "0,1,2,3,4,5,6,7,8,9") ?: "0,1,2,3,4,5,6,7,8,9"
        set(value) = prefs.edit().putString(KEY_ENABLED_CATEGORIES, value).apply()

    companion object {
        const val ENGINE_GOOGLE_ML = "google_mlkit"
        const val ENGINE_HUAWEI_HMS = "huawei_hms"
        const val ENGINE_TFLITE_YOLO = "tflite_yolo"

        const val ENGINE_ANDROID_TTS = "android_tts"
        const val ENGINE_HUAWEI_TTS = "huawei_tts"

        const val CAMERA_BACK = "back"
        const val CAMERA_FRONT = "front"

        // HMS_API_KEY should be loaded from local.properties or BuildConfig
        // NEVER commit real API keys to source control
        const val HMS_API_KEY = "" // Placeholder - load from BuildConfig.HMS_API_KEY in production

        private const val KEY_DETECTION_ENGINE = "detection_engine"
        private const val KEY_SPEECH_ENGINE = "speech_engine"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_REPORT_DISTANCE = "report_distance"
        private const val KEY_REPORT_DIRECTION = "report_direction"
        private const val KEY_REPORT_SIZE = "report_size"
        private const val KEY_REPORT_SCENE = "report_scene"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_MIN_CONFIDENCE = "min_confidence"
        private const val KEY_REPORT_INTERVAL = "report_interval"
        private const val KEY_VIBRATION_FEEDBACK = "vibration_feedback"
        private const val KEY_CAMERA_FACING = "camera_facing"
        private const val KEY_DISTANCE_THRESHOLD = "distance_threshold"
        private const val KEY_FOCUS_CLOSEST = "focus_closest"
        private const val KEY_ENABLED_CATEGORIES = "enabled_categories"
    }
}
