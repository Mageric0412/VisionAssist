package com.visionassist.detector

import android.graphics.Bitmap
import timber.log.Timber

/**
 * Fallback dummy detector when real detectors fail
 */
class DummyDetector : ObjectDetectorInterface {

    private var callback: ObjectDetectorInterface.DetectionCallback? = null
    private var initialized = false

    override fun initialize(callback: ObjectDetectorInterface.DetectionCallback) {
        this.callback = callback
        initialized = true
        Timber.w("Dummy detector initialized - no real detection will occur")
        callback.onInitialized()
    }

    override fun detect(bitmap: Bitmap, frameWidth: Int, frameHeight: Int) {
        if (!initialized) {
            callback?.onFailure("Dummy detector not initialized")
            return
        }

        // Return empty results
        callback?.onSuccess(emptyList())
    }

    override fun isReady(): Boolean = initialized

    override fun close() {
        initialized = false
    }

    override fun getName(): String = "Dummy (No Detection)"
}
