package com.visionassist.detector

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DetectedObject data class
 * Regression: Tests for direction calculation and area computation
 * Found by /qa on 2026-03-23
 */
class DetectedObjectTest {

    // ========== Geometry calculations ==========

    @Test
    fun `centerX is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 50f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        assertEquals(200f, obj.centerX, 0.01f)
    }

    @Test
    fun `centerY is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 50f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        assertEquals(225f, obj.centerY, 0.01f)
    }

    @Test
    fun `width is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 50f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        assertEquals(200f, obj.width, 0.01f)
    }

    @Test
    fun `height is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 50f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        assertEquals(350f, obj.height, 0.01f)
    }

    @Test
    fun `area is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f, top = 50f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // width = 200, height = 350, area = 70000
        assertEquals(70000f, obj.area, 0.01f)
    }

    // ========== Relative position tests ==========

    @Test
    fun `getRelativeX returns 0_5 for center horizontal`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 300f, top = 200f, right = 340f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // centerX = 320, frameWidth = 640, relativeX = 320/640 = 0.5
        assertEquals(0.5f, obj.getRelativeX(640), 0.01f)
    }

    @Test
    fun `getRelativeX returns correct value for left position`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 10f, top = 200f, right = 100f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // centerX = 55, frameWidth = 640, relativeX = 55/640 = 0.086
        assertEquals(0.086f, obj.getRelativeX(640), 0.01f)
    }

    @Test
    fun `getRelativeX returns correct value for right position`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 540f, top = 200f, right = 630f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // centerX = 585, frameWidth = 640, relativeX = 585/640 = 0.914
        assertEquals(0.914f, obj.getRelativeX(640), 0.01f)
    }

    // ========== Direction tests ==========

    @Test
    fun `getDirection returns LEFT for objects on left third`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 10f, top = 200f, right = 100f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.HorizontalDirection.LEFT, direction.horizontal)
    }

    @Test
    fun `getDirection returns RIGHT for objects on right third`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 540f, top = 200f, right = 630f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.HorizontalDirection.RIGHT, direction.horizontal)
    }

    @Test
    fun `getDirection returns CENTER for objects in middle third`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 250f, top = 200f, right = 390f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.HorizontalDirection.CENTER, direction.horizontal)
    }

    @Test
    fun `getDirection returns TOP for objects in top third`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 250f, top = 20f, right = 390f, bottom = 100f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.VerticalDirection.TOP, direction.vertical)
    }

    @Test
    fun `getDirection returns BOTTOM for objects in bottom third`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 250f, top = 350f, right = 390f, bottom = 470f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.VerticalDirection.BOTTOM, direction.vertical)
    }

    @Test
    fun `getDirection returns MIDDLE for objects in middle third vertically`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 250f, top = 180f, right = 390f, bottom = 300f,
            estimatedDistance = 2.0f
        )

        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.VerticalDirection.MIDDLE, direction.vertical)
    }

    // ========== Edge case tests ==========

    @Test
    fun `getDirection handles exact boundary at 0_33`() {
        // Object at exactly 33% (left boundary of CENTER)
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 200f, top = 200f, right = 300f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // centerX = 250, relativeX = 250/640 = 0.39 (CENTER)
        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.HorizontalDirection.CENTER, direction.horizontal)
    }

    @Test
    fun `getDirection handles exact boundary at 0_66`() {
        // Object at exactly 66% (right boundary of CENTER)
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 400f, top = 200f, right = 500f, bottom = 400f,
            estimatedDistance = 2.0f
        )

        // centerX = 450, relativeX = 450/640 = 0.70 (RIGHT)
        val direction = obj.getDirection(640, 480)

        assertEquals(DetectedObject.HorizontalDirection.RIGHT, direction.horizontal)
    }
}
