package com.visionassist.detector

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import com.visionassist.VisionAssistApp
import java.io.File
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * TFLite YOLOv8n Object Detector
 * High performance local inference, no network required
 * Model: yolov8n-int8.tflite (6MB, fast)
 */
class TFLiteYOLODetector(private val context: Context) : ObjectDetectorInterface {

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var callback: ObjectDetectorInterface.DetectionCallback? = null

    // Model input size (YOLOv8n uses 320x320)
    private val inputSize = 320
    private val pixelSize = 3 // RGB
    private val maxObjects = 25
    private val numClasses = 80 // COCO has 80 classes

    // YOLOv8 output format: [4(box coords) + 1(objectness) + 80(class probs)] = 85
    private val outputChannelSize = 4 + 1 + numClasses // 85

    // Confidence threshold
    private var minConfidence = 0.6f

    // Reusable buffers to reduce allocation
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * inputSize * inputSize * pixelSize).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    private val outputBuffer: Array<Array<FloatArray>> by lazy {
        Array(1) { Array(maxObjects) { FloatArray(outputChannelSize) } }
    }

    override fun initialize(callback: ObjectDetectorInterface.DetectionCallback) {
        this.callback = callback
        minConfidence = VisionAssistApp.getInstance().appConfig.minConfidence

        try {
            val modelPath = findModelFile()
            Timber.d("Loading YOLO model from: $modelPath")

            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = true
                setAllowFp16PrecisionForFp32(true)
            }

            interpreter = Interpreter(File(modelPath), options)
            isInitialized = true
            Timber.d("TFLite YOLO detector initialized")
            callback.onInitialized()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TFLite YOLO detector")
            callback.onFailure("TFLite initialization failed: ${e.message}")
        }
    }

    private fun findModelFile(): String {
        return try {
            context.assets.open("yolov8n.tflite").close()
            "yolov8n.tflite"
        } catch (e: Exception) {
            Timber.w("YOLO model not found in assets, will use fallback")
            "yolov8n.tflite"
        }
    }

    override fun detect(bitmap: Bitmap, frameWidth: Int, frameHeight: Int) {
        if (!isInitialized || interpreter == null) {
            callback?.onFailure("Detector not initialized")
            return
        }

        try {
            val startTime = System.currentTimeMillis()

            // Preprocess image into reusable buffer
            preprocessImage(bitmap)

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Post-process results
            val detectedObjects = postProcessResults(frameWidth, frameHeight)

            val inferenceTime = System.currentTimeMillis() - startTime
            Timber.v("YOLO inference: ${inferenceTime}ms, objects: ${detectedObjects.size}")

            callback?.onSuccess(detectedObjects)
        } catch (e: Exception) {
            Timber.e(e, "YOLO detection failed")
            callback?.onFailure(e.message ?: "YOLO detection failed")
        }
    }

    private fun preprocessImage(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        inputBuffer.clear()

        val pixels = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            // YOLOv8 uses normalized RGB [0, 1]
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        scaledBitmap.recycle()
    }

    private fun postProcessResults(
        frameWidth: Int,
        frameHeight: Int
    ): List<DetectedObject> {
        val results = mutableListOf<DetectedObject>()
        val scaleX = frameWidth.toFloat() / inputSize
        val scaleY = frameHeight.toFloat() / inputSize

        for (output in outputBuffer[0]) {
            // YOLOv8 output: [x, y, w, h, obj_score, class0_score, class1_score, ...]
            val objScore = output[4]
            if (objScore < minConfidence) continue

            // Find the class with highest probability
            var maxClassProb = 0f
            var classId = 0
            for (c in 0 until numClasses) {
                val classProb = output[5 + c]
                if (classProb > maxClassProb) {
                    maxClassProb = classProb
                    classId = c
                }
            }

            val confidence = objScore * maxClassProb
            if (confidence < minConfidence) continue

            // Decode box coordinates (center x, center y, width, height)
            val cx = output[0] * scaleX
            val cy = output[1] * scaleY
            val w = output[2] * scaleX
            val h = output[3] * scaleY

            val left = (cx - w / 2).coerceIn(0f, frameWidth.toFloat())
            val top = (cy - h / 2).coerceIn(0f, frameHeight.toFloat())
            val right = (cx + w / 2).coerceIn(0f, frameWidth.toFloat())
            val bottom = (cy + h / 2).coerceIn(0f, frameHeight.toFloat())

            results.add(
                DetectedObject(
                    label = COCOLabels.getLabel(classId),
                    labelIndex = classId,
                    confidence = confidence,
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    estimatedDistance = estimateDistance(w, h, frameWidth, frameHeight, classId)
                )
            )
        }

        return results.sortedByDescending { it.confidence }.take(10)
    }

    private fun estimateDistance(
        objWidth: Float,
        objHeight: Float,
        frameWidth: Int,
        frameHeight: Int,
        labelIndex: Int
    ): Float {
        val objArea = objWidth * objHeight
        val frameArea = frameWidth * frameHeight
        val ratio = objArea / frameArea

        val refSize = if (COCOLabels.obstacleLabels.contains(labelIndex)) 0.3f else 0.2f

        return if (ratio > 0) {
            max(0.3f, refSize / sqrt(ratio))
        } else {
            5f
        }
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Timber.d("TFLite YOLO detector closed")
    }

    override fun getName(): String = "TFLite YOLOv8n"
}
