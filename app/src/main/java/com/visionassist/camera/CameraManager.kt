package com.visionassist.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.visionassist.VisionAssistApp
import com.visionassist.config.AppConfig
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera manager using CameraX for high-performance frame capture.
 * Provides frames to registered callbacks for processing.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var frameCallback: FrameCallback? = null
    private var isStarted = false

    private val config get() = VisionAssistApp.getInstance().appConfig

    // Target frame rate for analysis
    private val targetFps = 15

    // Frame processing state
    @Volatile
    private var lastFrameTimestamp = 0L

    interface FrameCallback {
        /**
         * Called for each captured frame.
         * The bitmap is provided for immediate processing only.
         * The callback should NOT store the bitmap reference.
         * @param bitmap Frame bitmap (do not store or recycle)
         * @param timestamp Frame timestamp in nanoseconds
         */
        fun onFrame(bitmap: Bitmap, timestamp: Long)
    }

    fun setFrameCallback(callback: FrameCallback?) {
        this.frameCallback = callback
    }

    /**
     * Get the preview object for binding to PreviewView
     */
    fun getPreview(): Preview? = preview

    @RequiresPermission(Manifest.permission.CAMERA)
    fun start() {
        if (isStarted) {
            Timber.w("Camera already started")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                isStarted = true
                Timber.d("Camera started successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = if (config.cameraFacing == AppConfig.CAMERA_BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        preview = Preview.Builder()
            .setTargetResolution(Size(640, 480))
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            Timber.d("Camera use cases bound")
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind camera use cases")
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()

        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                val timestamp = imageProxy.imageInfo.timestamp

                // Rate limit frames
                if (timestamp - lastFrameTimestamp >= frameIntervalNanos) {
                    lastFrameTimestamp = timestamp
                    frameCallback?.onFrame(bitmap, timestamp)
                }

                // Recycle bitmap after callback completes
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing image")
        } finally {
            imageProxy.close()
        }

        Timber.v("Frame processing: ${System.currentTimeMillis() - startTime}ms")
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + vSize + uSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 85, out)

        val rawBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()) ?: return null

        // Apply rotation if needed
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            rawBitmap.recycle() // Recycle original after creating rotated copy
            rotated
        } else {
            rawBitmap
        }

        return bitmap
    }

    fun stop() {
        cameraProvider?.unbindAll()
        isStarted = false
        Timber.d("Camera stopped")
    }

    fun release() {
        stop()
        cameraExecutor.shutdown()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val frameIntervalNanos: Long
        get() = (1_000_000_000L / targetFps)

    companion object {
        private const val TAG = "CameraManager"
    }
}
