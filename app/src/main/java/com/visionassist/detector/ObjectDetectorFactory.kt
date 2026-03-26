package com.visionassist.detector

import android.content.Context
import timber.log.Timber

/**
 * Factory for creating object detectors based on configuration.
 * Supports engine switching at runtime.
 */
class ObjectDetectorFactory(private val context: Context) {

    private var currentDetector: ObjectDetectorInterface? = null
    private var currentEngine: String? = null

    fun createDetector(engine: String): ObjectDetectorInterface {
        Timber.d("Creating detector with engine: $engine")

        currentDetector?.close()

        val detector = try {
            when (engine) {
                Engine.GOOGLE_ML -> GoogleMLKitDetector(context)
                Engine.HUAWEI_HMS -> HuaweiHMSDetector(context)
                Engine.TFLITE_YOLO -> TFLiteYOLODetector(context)
                else -> {
                    Timber.w("Unknown engine $engine, using Google ML Kit")
                    GoogleMLKitDetector(context)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create $engine detector, using fallback")
            createFallbackDetector()
        }

        currentDetector = detector
        currentEngine = engine
        return detector
    }

    private fun createFallbackDetector(): ObjectDetectorInterface {
        return try {
            GoogleMLKitDetector(context)
        } catch (e: Exception) {
            Timber.e(e, "Fallback detector also failed")
            DummyDetector()
        }
    }

    fun getCurrentDetector(): ObjectDetectorInterface? = currentDetector

    fun getCurrentEngine(): String? = currentEngine

    object Engine {
        const val GOOGLE_ML = "google_mlkit"
        const val HUAWEI_HMS = "huawei_hms"
        const val TFLITE_YOLO = "tflite_yolo"
    }
}
