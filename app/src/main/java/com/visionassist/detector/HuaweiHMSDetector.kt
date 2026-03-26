package com.visionassist.detector

import android.content.Context
import android.graphics.Bitmap
import timber.log.Timber

/**
 * Huawei HMS ML Kit Object Detector
 * Provides excellent performance on Huawei devices
 *
 * NOTE: HMS dependency is disabled. This detector returns empty results.
 * To enable, uncomment HMS dependencies in build.gradle and rebuild.
 */
class HuaweiHMSDetector(private val context: Context) : ObjectDetectorInterface {

    private var isInitialized = false
    private var callback: ObjectDetectorInterface.DetectionCallback? = null

    override fun initialize(callback: ObjectDetectorInterface.DetectionCallback) {
        this.callback = callback
        // HMS dependency disabled — provide graceful fallback
        isInitialized = true
        Timber.d("Huawei HMS detector initialized (stub mode — HMS disabled)")
        callback.onInitialized()
    }

    override fun detect(bitmap: Bitmap, frameWidth: Int, frameHeight: Int) {
        if (!isInitialized) {
            callback?.onFailure("Detector not initialized")
            return
        }
        // HMS disabled — return empty results
        callback?.onSuccess(emptyList())
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        isInitialized = false
        Timber.d("Huawei HMS detector closed")
    }

    override fun getName(): String = "Huawei HMS ML Kit"
}
