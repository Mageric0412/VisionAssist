package com.visionassist.analysis

import android.graphics.Bitmap
import android.graphics.Color
import com.visionassist.detector.DetectedObject
import com.visionassist.detector.SceneAnalysis
import timber.log.Timber

/**
 * Analyzes the scene context beyond just object detection.
 * Provides environment understanding for better navigation assistance.
 */
class SceneAnalyzer {

    /**
     * Analyze a frame and return scene context
     */
    fun analyze(bitmap: Bitmap, detectedObjects: List<DetectedObject>): SceneAnalysis {
        val environment = detectEnvironment(bitmap)
        val lighting = detectLighting(bitmap)

        return SceneAnalysis(
            environment = environment,
            lighting = lighting,
            objects = detectedObjects
        )
    }

    /**
     * Detect if indoor or outdoor
     */
    private fun detectEnvironment(bitmap: Bitmap): SceneAnalysis.Environment {
        val samples = samplePixels(bitmap, 8)

        // Analyze sky color (top portion)
        val skyCount = samples.count { isSkyColor(it) }

        // Analyze green presence (vegetation)
        val greenCount = samples.count { isGreenish(it) }

        // Analyze ground colors (brown/gray)
        val groundCount = samples.count { isGroundColor(it) }

        // Analyze ceiling (uniform light colors in top portion)
        val ceilingCount = samples.count { isCeilingColor(it) }

        return when {
            skyCount >= 3 -> SceneAnalysis.Environment.OUTDOOR
            greenCount > 10 -> SceneAnalysis.Environment.OUTDOOR // Park/nature
            ceilingCount > 15 && groundCount > 5 -> SceneAnalysis.Environment.INDOOR
            groundCount > 12 -> SceneAnalysis.Environment.INDOOR
            else -> SceneAnalysis.Environment.UNKNOWN
        }
    }

    /**
     * Sample pixels from the bitmap grid
     */
    private fun samplePixels(bitmap: Bitmap, gridSize: Int): List<Int> {
        val samples = mutableListOf<Int>()
        val stepX = bitmap.width / (gridSize + 1)
        val stepY = bitmap.height / (gridSize + 1)

        for (x in 1..gridSize) {
            for (y in 1..gridSize) {
                try {
                    val pixel = bitmap.getPixel(x * stepX, y * stepY)
                    samples.add(pixel)
                } catch (e: Exception) {
                    Timber.v("samplePixels: ignoring out of bounds at ($x, $y)")
                }
            }
        }
        return samples
    }

    /**
     * Detect lighting conditions
     */
    private fun detectLighting(bitmap: Bitmap): SceneAnalysis.Lighting {
        // Sample every 16th pixel for performance
        var totalBrightness = 0L
        var pixelCount = 0

        for (x in 0 until bitmap.width step 16) {
            for (y in 0 until bitmap.height step 16) {
                try {
                    val pixel = bitmap.getPixel(x, y)
                    totalBrightness += getBrightness(pixel)
                    pixelCount++
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        if (pixelCount == 0) return SceneAnalysis.Lighting.UNKNOWN

        val avgBrightness = totalBrightness.toFloat() / pixelCount

        return when {
            avgBrightness > 180 -> SceneAnalysis.Lighting.BRIGHT
            avgBrightness > 100 -> SceneAnalysis.Lighting.NORMAL
            avgBrightness > 40 -> SceneAnalysis.Lighting.LOW
            else -> SceneAnalysis.Lighting.LOW
        }
    }

    private fun isSkyColor(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return b > r && b > (g * 0.8f) && b > 150
    }

    private fun isGreenish(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return g > (r * 1.2f) && g > (b * 1.2f) && g > 80
    }

    private fun isGroundColor(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val brightness = (r + g + b) / 3f
        val isGrayish = kotlin.math.abs(r - g) < 30 && kotlin.math.abs(g - b) < 30
        return brightness in 40f..160f && isGrayish
    }

    private fun isCeilingColor(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val brightness = (r + g + b) / 3f
        val isUniform = kotlin.math.abs(r - g) < 20 && kotlin.math.abs(g - b) < 20
        return brightness > 150 && isUniform
    }

    private fun getBrightness(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299f * r + 0.587f * g + 0.114f * b).toInt()
    }

    /**
     * Generate scene description for speech
     */
    fun generateSceneDescription(analysis: SceneAnalysis): String {
        val parts = mutableListOf<String>()

        when (analysis.environment) {
            SceneAnalysis.Environment.INDOOR -> parts.add("Indoor environment")
            SceneAnalysis.Environment.OUTDOOR -> parts.add("Outdoor environment")
            SceneAnalysis.Environment.UNKNOWN -> {}
        }

        when (analysis.lighting) {
            SceneAnalysis.Lighting.BRIGHT -> parts.add("bright lighting")
            SceneAnalysis.Lighting.NORMAL -> {}
            SceneAnalysis.Lighting.LOW -> parts.add("low light, be careful")
            SceneAnalysis.Lighting.UNKNOWN -> {}
        }

        val objectCount = analysis.objects.size
        when {
            objectCount == 0 -> parts.add("no obstacles detected")
            objectCount == 1 -> parts.add("1 obstacle ahead")
            objectCount in 2..3 -> parts.add("$objectCount obstacles nearby")
            else -> parts.add("multiple obstacles around")
        }

        return parts.joinToString(". ") + "."
    }
}
