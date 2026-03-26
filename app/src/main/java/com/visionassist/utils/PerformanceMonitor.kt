package com.visionassist.utils

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance monitoring utility for tracking FPS, latency, etc.
 */
object PerformanceMonitor {

    private val frameTimes = ConcurrentHashMap<Long, Long>() // timestamp -> processing time
    private val detectionTimes = ConcurrentHashMap<Long, Long>()

    private var totalFrames = 0L
    private var droppedFrames = 0L

    fun recordFrameProcessing(startTimeMs: Long) {
        val processingTime = System.currentTimeMillis() - startTimeMs
        frameTimes[System.currentTimeMillis()] = processingTime
        totalFrames++

        // Clean old entries (keep last 100)
        if (frameTimes.size > 100) {
            val oldest = frameTimes.keys().toList().sorted().take(frameTimes.size - 100)
            oldest.forEach { frameTimes.remove(it) }
        }
    }

    fun recordDetection(detectionTimeMs: Long) {
        detectionTimes[System.currentTimeMillis()] = detectionTimeMs

        if (detectionTimes.size > 100) {
            val oldest = detectionTimes.keys().toList().sorted().take(detectionTimes.size - 100)
            oldest.forEach { detectionTimes.remove(it) }
        }
    }

    fun recordDroppedFrame() {
        droppedFrames++
    }

    fun getAverageFrameTime(): Float {
        if (frameTimes.isEmpty()) return 0f
        return frameTimes.values.average().toFloat()
    }

    fun getAverageFps(): Float {
        val avgTime = getAverageFrameTime()
        return if (avgTime > 0) 1000f / avgTime else 0f
    }

    fun getAverageDetectionTime(): Float {
        if (detectionTimes.isEmpty()) return 0f
        return detectionTimes.values.average().toFloat()
    }

    fun getTotalFrames(): Long = totalFrames

    fun getDroppedFrames(): Long = droppedFrames

    fun getDropRate(): Float {
        return if (totalFrames > 0) droppedFrames.toFloat() / totalFrames else 0f
    }

    fun reset() {
        frameTimes.clear()
        detectionTimes.clear()
        totalFrames = 0L
        droppedFrames = 0L
    }

    fun logStats() {
        Timber.d("Performance Stats:")
        Timber.d("  Average FPS: ${"%.1f".format(getAverageFps())}")
        Timber.d("  Average Frame Time: ${"%.1f".format(getAverageFrameTime())}ms")
        Timber.d("  Average Detection Time: ${"%.1f".format(getAverageDetectionTime())}ms")
        Timber.d("  Total Frames: $totalFrames")
        Timber.d("  Dropped Frames: $droppedFrames (${"%.1f".format(getDropRate() * 100)}%)")
    }

    class PerformanceSnapshot(
        val fps: Float,
        val avgFrameTime: Float,
        val avgDetectionTime: Float,
        val totalFrames: Long,
        val droppedFrames: Long,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun getSnapshot(): PerformanceSnapshot {
        return PerformanceSnapshot(
            fps = getAverageFps(),
            avgFrameTime = getAverageFrameTime(),
            avgDetectionTime = getAverageDetectionTime(),
            totalFrames = totalFrames,
            droppedFrames = droppedFrames
        )
    }
}
