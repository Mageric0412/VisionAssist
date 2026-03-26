package com.visionassist.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

/**
 * Test utilities for processing video/image files locally
 * Used for offline testing without camera
 */
object VideoTestSource {

    private var mediaRetriever = MediaMetadataRetriever()
    private var videoFile: File? = null
    private var videoDurationMs: Long = 0
    private var currentPositionMs: Long = 0
    private var isPlaying = false

    /**
     * Open a video file for testing
     */
    fun openVideo(path: String): Boolean {
        return try {
            // Release existing resources before opening new file
            close()

            videoFile = File(path)
            if (videoFile?.exists() == true) {
                mediaRetriever.setDataSource(path)
                videoDurationMs = mediaRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0
                currentPositionMs = 0
                isPlaying = false
                Timber.d("Opened video: $path, duration: ${videoDurationMs}ms")
                true
            } else {
                Timber.w("Video file not found: $path")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open video")
            false
        }
    }

    /**
     * Get frame at current position
     */
    fun getCurrentFrame(): Bitmap? {
        return try {
            mediaRetriever.getFrameAtTime(
                currentPositionMs * 1000, // Convert to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get frame")
            null
        }
    }

    /**
     * Get frame at specific position
     */
    fun getFrameAt(positionMs: Long): Bitmap? {
        return try {
            mediaRetriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get frame at $positionMs")
            null
        }
    }

    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        currentPositionMs = positionMs.coerceIn(0, videoDurationMs)
    }

    /**
     * Advance by delta ms
     */
    fun advance(deltaMs: Long) {
        currentPositionMs = (currentPositionMs + deltaMs).coerceIn(0, videoDurationMs)
    }

    /**
     * Close resources
     */
    fun close() {
        try {
            mediaRetriever.release()
        } catch (e: Exception) {
            Timber.e(e, "Error closing MediaMetadataRetriever")
        }
        videoFile = null
    }

    fun getDuration(): Long = videoDurationMs
    fun getPosition(): Long = currentPositionMs
    fun getProgress(): Float = if (videoDurationMs > 0) currentPositionMs.toFloat() / videoDurationMs else 0f

    /**
     * Process a static image file
     */
    fun loadImage(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load image: $path")
            null
        }
    }

    /**
     * Get all frames from video at interval
     */
    suspend fun extractFrames(
        intervalMs: Long = 100,
        onFrame: (Bitmap, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val frameCount = videoDurationMs / intervalMs
        for (i in 0 until frameCount) {
            val position = i * intervalMs
            getFrameAt(position)?.let { bitmap ->
                onFrame(bitmap, position)
            }
            delay(1) // Yield to avoid blocking
        }
    }
}

/**
 * Sample test images for offline testing
 * Place test images in assets/test_images/
 */
object TestImages {
    private val testImageNames = listOf(
        "indoor_obstacle.jpg",
        "outdoor_pedestrian.jpg",
        "street_scene.jpg",
        "stairs_up.jpg",
        "stairs_down.jpg"
    )

    fun getAvailableTestImages(context: Context): List<String> {
        return testImageNames.filter { name ->
            try {
                context.assets.open("test_images/$name").close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
