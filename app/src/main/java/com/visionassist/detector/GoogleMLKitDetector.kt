package com.visionassist.detector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Google ML Kit Object Detector
 * Fast, on-device detection with good accuracy
 */
class GoogleMLKitDetector(private val context: Context) : ObjectDetectorInterface {

    private var detector: ObjectDetector? = null
    private var isInitialized = false
    private var callback: ObjectDetectorInterface.DetectionCallback? = null

    /** Average object sizes in meters for distance estimation */
    private val avgObjectSizes = mapOf(
        0 to 0.5f,   // person
        2 to 1.5f,   // car
        3 to 0.8f,   // motorcycle
        5 to 2.5f,   // bus
        7 to 2.0f    // truck
    )

    override fun initialize(callback: ObjectDetectorInterface.DetectionCallback) {
        this.callback = callback

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()

        detector = ObjectDetection.getClient(options)
        isInitialized = true
        Timber.d("Google ML Kit detector initialized")
        callback.onInitialized()
    }

    override fun detect(bitmap: Bitmap, frameWidth: Int, frameHeight: Int) {
        if (!isInitialized || detector == null) {
            callback?.onFailure("Detector not initialized")
            return
        }

        // Make a copy since ML Kit processing is async and bitmap may be recycled
        val bitmapCopy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        if (bitmapCopy == null) {
            callback?.onFailure("Failed to copy bitmap")
            return
        }

        val inputImage = InputImage.fromBitmap(bitmapCopy, 0)

        detector?.process(inputImage)
            ?.addOnSuccessListener { visionObjects ->
                // Recycle the copy after processing
                bitmapCopy.recycle()

                val detectedObjects = visionObjects.mapIndexed { visionIdx, obj ->
                    val bounds = obj.boundingBox
                    // Use first label's index if available, otherwise use list position as fallback
                    val labelIndex = obj.labels.firstOrNull()?.let { label ->
                        COCOLabels.getIndex(label.text).takeIf { it >= 0 } ?: visionIdx
                    } ?: visionIdx
                    val label = obj.labels.firstOrNull()?.text
                        ?: COCOLabels.getLabel(labelIndex)
                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0.5f

                    DetectedObject(
                        label = label,
                        labelIndex = labelIndex,
                        confidence = confidence,
                        left = bounds.left.toFloat(),
                        top = bounds.top.toFloat(),
                        right = bounds.right.toFloat(),
                        bottom = bounds.bottom.toFloat(),
                        estimatedDistance = estimateDistance(
                            bounds.width(),
                            bounds.height(),
                            frameWidth,
                            frameHeight,
                            labelIndex
                        )
                    )
                }
                callback?.onSuccess(detectedObjects)
            }
            ?.addOnFailureListener { e ->
                bitmapCopy.recycle()
                Timber.e(e, "ML Kit detection failed")
                callback?.onFailure(e.message ?: "Detection failed")
            }
    }

    private fun estimateDistance(
        objWidth: Int,
        objHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        labelIndex: Int
    ): Float {
        val knownSize = avgObjectSizes[labelIndex] ?: 0.5f
        val pixelSize = (objWidth + objHeight) / 2f
        val frameDiagonal = sqrt((frameWidth * frameWidth + frameHeight * frameHeight).toFloat())
        val ratio = pixelSize / frameDiagonal

        return if (ratio > 0) (knownSize / ratio) else 5f
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        detector?.close()
        detector = null
        isInitialized = false
        Timber.d("Google ML Kit detector closed")
    }

    override fun getName(): String = "Google ML Kit"
}
