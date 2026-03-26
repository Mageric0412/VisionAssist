package com.visionassist.utils

import com.visionassist.detector.DetectedObject
import com.visionassist.detector.SceneAnalysis
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AnnouncementBuilder
 * Regression: Tests for distance formatting and direction text generation
 * Found by /qa on 2026-03-23
 */
class AnnouncementBuilderTest {

    // ========== formatDistance tests ==========

    @Test
    fun `formatDistance returns very close danger for meters less than 0_3`() {
        assertEquals("very close, immediate danger", AnnouncementBuilder.formatDistance(0.1f))
        assertEquals("very close, immediate danger", AnnouncementBuilder.formatDistance(0.29f))
    }

    @Test
    fun `formatDistance returns very close for meters 0_3 to 0_5`() {
        assertEquals("very close", AnnouncementBuilder.formatDistance(0.3f))
        assertEquals("very close", AnnouncementBuilder.formatDistance(0.49f))
    }

    @Test
    fun `formatDistance returns centimeters away for meters 0_5 to 1_0`() {
        assertEquals("50 centimeters away", AnnouncementBuilder.formatDistance(0.5f))
        assertEquals("75 centimeters away", AnnouncementBuilder.formatDistance(0.75f))
        assertEquals("99 centimeters away", AnnouncementBuilder.formatDistance(0.99f))
    }

    @Test
    fun `formatDistance returns meter ahead for meters 1_0 to 3_0`() {
        assertEquals("about 1 meter ahead", AnnouncementBuilder.formatDistance(1.0f))
        assertEquals("about 1 meter ahead", AnnouncementBuilder.formatDistance(1.5f))
        assertEquals("about 2 meters ahead", AnnouncementBuilder.formatDistance(2.0f))
        assertEquals("about 2 meters ahead", AnnouncementBuilder.formatDistance(2.5f))
        assertEquals("about 3 meters ahead", AnnouncementBuilder.formatDistance(3.0f))
    }

    @Test
    fun `formatDistance returns 4-5 meters ahead correctly`() {
        assertEquals("about 4 meters ahead", AnnouncementBuilder.formatDistance(4.0f))
        assertEquals("about 4 meters ahead", AnnouncementBuilder.formatDistance(4.5f))
        assertEquals("about 5 meters ahead", AnnouncementBuilder.formatDistance(5.0f))
        assertEquals("about 5 meters ahead", AnnouncementBuilder.formatDistance(6.5f))
    }

    @Test
    fun `formatDistance returns far for meters 7_0 to 10_0`() {
        assertEquals("far, about 8 meters", AnnouncementBuilder.formatDistance(7.0f))
        assertEquals("far, about 8 meters", AnnouncementBuilder.formatDistance(8.0f))
        assertEquals("far, about 8 meters", AnnouncementBuilder.formatDistance(9.5f))
    }

    @Test
    fun `formatDistance returns very far for meters over 10`() {
        assertEquals("very far, about 15 meters", AnnouncementBuilder.formatDistance(15.0f))
        assertEquals("very far, about 100 meters", AnnouncementBuilder.formatDistance(100.0f))
    }

    // ========== buildObstacleAnnouncement tests ==========

    @Test
    fun `buildObstacleAnnouncement includes label and direction`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 100f, right = 200f, bottom = 400f,
            estimatedDistance = 2.5f
        )

        val result = AnnouncementBuilder.buildObstacleAnnouncement(obj, 640, 480)

        assertTrue(result.contains("person"))
        assertTrue(result.contains("about 2 meters ahead"))
    }

    @Test
    fun `buildObstacleAnnouncement includes direction LEFT`() {
        // Object on left side of frame (centerX at ~10% of width)
        val obj = DetectedObject(
            label = "car",
            labelIndex = 2,
            confidence = 0.8f,
            left = 10f, top = 200f, right = 100f, bottom = 350f,
            estimatedDistance = 3.0f
        )

        val result = AnnouncementBuilder.buildObstacleAnnouncement(obj, 640, 480)

        assertTrue(result.contains("car"))
        assertTrue(result.contains("on your left"))
    }

    @Test
    fun `buildObstacleAnnouncement includes direction RIGHT`() {
        // Object on right side of frame (centerX at ~90% of width)
        val obj = DetectedObject(
            label = "bicycle",
            labelIndex = 1,
            confidence = 0.85f,
            left = 550f, top = 200f, right = 630f, bottom = 400f,
            estimatedDistance = 1.5f
        )

        val result = AnnouncementBuilder.buildObstacleAnnouncement(obj, 640, 480)

        assertTrue(result.contains("bicycle"))
        assertTrue(result.contains("on your right"))
    }

    @Test
    fun `buildObstacleAnnouncement handles null distance`() {
        val obj = DetectedObject(
            label = "bench",
            labelIndex = 13,
            confidence = 0.7f,
            left = 200f, top = 150f, right = 440f, bottom = 350f,
            estimatedDistance = null
        )

        val result = AnnouncementBuilder.buildObstacleAnnouncement(obj, 640, 480)

        assertTrue(result.contains("bench"))
        // Should not contain "about X meters" since distance is null
        assertFalse(result.contains("meter"))
    }

    // ========== buildMultipleObjectsAnnouncement tests ==========

    @Test
    fun `buildMultipleObjectsAnnouncement returns empty for empty list`() {
        assertEquals("", AnnouncementBuilder.buildMultipleObjectsAnnouncement(emptyList()))
    }

    @Test
    fun `buildMultipleObjectsAnnouncement returns single object correctly`() {
        val objects = listOf(
            DetectedObject("person", 0, 0.9f, 0f, 0f, 100f, 100f, 2.0f)
        )

        val result = AnnouncementBuilder.buildMultipleObjectsAnnouncement(objects)

        assertEquals("1 person ahead", result)
    }

    @Test
    fun `buildMultipleObjectsAnnouncement returns two objects with and`() {
        val objects = listOf(
            DetectedObject("person", 0, 0.9f, 0f, 0f, 100f, 100f, 2.0f),
            DetectedObject("car", 2, 0.8f, 200f, 0f, 400f, 200f, 5.0f)
        )

        val result = AnnouncementBuilder.buildMultipleObjectsAnnouncement(objects)

        assertEquals("2 obstacles: person and car", result)
    }

    @Test
    fun `buildMultipleObjectsAnnouncement limits types to 3`() {
        val objects = listOf(
            DetectedObject("person", 0, 0.9f, 0f, 0f, 100f, 100f, 2.0f),
            DetectedObject("car", 2, 0.8f, 200f, 0f, 400f, 200f, 5.0f),
            DetectedObject("bicycle", 1, 0.7f, 400f, 0f, 500f, 150f, 3.0f),
            DetectedObject("dog", 16, 0.6f, 100f, 200f, 200f, 350f, 2.0f)
        )

        val result = AnnouncementBuilder.buildMultipleObjectsAnnouncement(objects)

        assertEquals("4 obstacles including person, car, bicycle", result)
    }

    @Test
    fun `buildMultipleObjectsAnnouncement returns multiple for 6+ objects`() {
        val objects = listOf(
            DetectedObject("person", 0, 0.9f, 0f, 0f, 100f, 100f, 2.0f),
            DetectedObject("car", 2, 0.8f, 200f, 0f, 400f, 200f, 5.0f),
            DetectedObject("bicycle", 1, 0.7f, 400f, 0f, 500f, 150f, 3.0f),
            DetectedObject("dog", 16, 0.6f, 100f, 200f, 200f, 350f, 2.0f),
            DetectedObject("tree", 0, 0.5f, 500f, 100f, 600f, 400f, 10.0f),
            DetectedObject("bench", 13, 0.4f, 300f, 200f, 400f, 350f, 4.0f)
        )

        val result = AnnouncementBuilder.buildMultipleObjectsAnnouncement(objects)

        assertEquals("multiple obstacles around you", result)
    }

    // ========== buildSceneAnnouncement tests ==========

    @Test
    fun `buildSceneAnnouncement returns indoor for indoor environment`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.INDOOR,
            lighting = SceneAnalysis.Lighting.NORMAL,
            objects = emptyList()
        )

        val result = AnnouncementBuilder.buildSceneAnnouncement(analysis)

        assertTrue(result.contains("Indoor area"))
    }

    @Test
    fun `buildSceneAnnouncement returns outdoor for outdoor environment`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.OUTDOOR,
            lighting = SceneAnalysis.Lighting.BRIGHT,
            objects = emptyList()
        )

        val result = AnnouncementBuilder.buildSceneAnnouncement(analysis)

        assertTrue(result.contains("Outdoor area"))
        assertTrue(result.contains("bright lighting"))
    }

    @Test
    fun `buildSceneAnnouncement warns about low lighting`() {
        val analysis = SceneAnalysis(
            environment = SceneAnalysis.Environment.INDOOR,
            lighting = SceneAnalysis.Lighting.LOW,
            objects = emptyList()
        )

        val result = AnnouncementBuilder.buildSceneAnnouncement(analysis)

        assertTrue(result.contains("low lighting"))
        assertTrue(result.contains("be careful"))
    }
}
