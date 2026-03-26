package com.visionassist.analysis

import android.graphics.Bitmap
import com.visionassist.detector.DetectedObject
import com.visionassist.detector.SceneAnalysis
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SceneAnalyzer
 * Regression: Tests for environment and lighting detection logic
 * Found by /qa on 2026-03-23
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SceneAnalyzerTest {

    private val sceneAnalyzer = SceneAnalyzer()

    // ========== Environment detection tests ==========

    @Test
    fun `analyze returns correct SceneAnalysis structure`() {
        val bitmap = createMockBitmap(640, 480)
        val objects = listOf(
            DetectedObject("person", 0, 0.9f, 100f, 100f, 200f, 300f, 2.0f)
        )

        val result = sceneAnalyzer.analyze(bitmap, objects)

        assertNotNull(result)
        assertEquals(objects, result.objects)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `analyze handles empty objects list`() {
        val bitmap = createMockBitmap(640, 480)

        val result = sceneAnalyzer.analyze(bitmap, emptyList())

        assertNotNull(result)
        assertTrue(result.objects.isEmpty())
    }

    @Test
    fun `generateSceneDescription returns indoor for indoor environment`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.INDOOR,
            lighting = SceneAnalysis.Lighting.NORMAL,
            objects = emptyList()
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("Indoor environment"))
    }

    @Test
    fun `generateSceneDescription returns outdoor for outdoor environment`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = emptyList()
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("Outdoor environment"))
    }

    @Test
    fun `generateSceneDescription warns about low light`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.INDOOR,
            lighting = SceneAnalysis.Lighting.LOW,
            objects = emptyList()
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("low light"))
        assertTrue(result.contains("be careful"))
    }

    @Test
    fun `generateSceneDescription does not mention normal lighting`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.INDOOR,
            lighting = SceneAnalysis.Lighting.NORMAL,
            objects = emptyList()
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        // NORMAL lighting should not be mentioned in description
        assertFalse(result.contains("normal"))
    }

    // ========== Object count in scene description ==========

    @Test
    fun `generateSceneDescription says no obstacles for empty objects`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = emptyList()
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("no obstacles detected"))
    }

    @Test
    fun `generateSceneDescription says 1 obstacle correctly`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = listOf(
                DetectedObject("person", 0, 0.9f, 100f, 100f, 200f, 300f, 2.0f)
            )
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("1 obstacle ahead"))
    }

    @Test
    fun `generateSceneDescription says 2-3 obstacles correctly`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = listOf(
                DetectedObject("person", 0, 0.9f, 100f, 100f, 200f, 300f, 2.0f),
                DetectedObject("car", 2, 0.8f, 300f, 100f, 500f, 300f, 5.0f)
            )
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("2 obstacles nearby"))
    }

    @Test
    fun `generateSceneDescription says multiple for 4+ obstacles`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = listOf(
                DetectedObject("person", 0, 0.9f, 100f, 100f, 200f, 300f, 2.0f),
                DetectedObject("car", 2, 0.8f, 300f, 100f, 500f, 300f, 5.0f),
                DetectedObject("bicycle", 1, 0.7f, 400f, 150f, 500f, 350f, 3.0f),
                DetectedObject("tree", 0, 0.5f, 50f, 200f, 100f, 400f, 10.0f)
            )
        )

        val result = sceneAnalyzer.generateSceneDescription(analysis)

        assertTrue(result.contains("multiple obstacles around"))
    }

    // ========== Lighting enum tests ==========

    @Test
    fun `SceneAnalysis Lighting enum has all expected values`() {
        assertEquals(4, SceneAnalysis.Lighting.entries.size)
        assertNotNull(SceneAnalysis.Lighting.BRIGHT)
        assertNotNull(SceneAnalysis.Lighting.NORMAL)
        assertNotNull(SceneAnalysis.Lighting.LOW)
        assertNotNull(SceneAnalysis.Lighting.UNKNOWN)
    }

    @Test
    fun `SceneAnalysis Environment enum has all expected values`() {
        assertEquals(3, SceneAnalysis.Environment.entries.size)
        assertNotNull(SceneAnalysis.Environment.INDOOR)
        assertNotNull(SceneAnalysis.Environment.OUTDOOR)
        assertNotNull(SceneAnalysis.Environment.UNKNOWN)
    }

    // ========== Helper functions ==========

    private fun createMockBitmap(width: Int, height: Int): Bitmap {
        val bitmap = mock(Bitmap::class.java)
        `when`(bitmap.width).thenReturn(width)
        `when`(bitmap.height).thenReturn(height)
        return bitmap
    }
}
