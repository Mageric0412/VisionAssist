package com.visionassist.detector

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DetectedObject direction calculations
 */
class NavigationDirectionTest {

    @Test
    fun `centerX is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f,
            top = 50f,
            right = 300f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        // centerX = (left + right) / 2 = (100 + 300) / 2 = 200
        assertEquals(200f, obj.centerX, 0.01f)
    }

    @Test
    fun `centerY is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f,
            top = 50f,
            right = 300f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        // centerY = (top + bottom) / 2 = (50 + 400) / 2 = 225
        assertEquals(225f, obj.centerY, 0.01f)
    }

    @Test
    fun `getRelativeX returns normalized value`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 160f,
            top = 50f,
            right = 480f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        // centerX = (160 + 480) / 2 = 320
        // getRelativeX(640) = 320 / 640 = 0.5
        assertEquals(0.5f, obj.getRelativeX(640), 0.01f)
    }

    @Test
    fun `getRelativeX for left side of frame`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 0f,
            top = 50f,
            right = 200f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        // centerX = (0 + 200) / 2 = 100
        // getRelativeX(640) = 100 / 640 = 0.156
        assertEquals(0.15625f, obj.getRelativeX(640), 0.01f)
    }

    @Test
    fun `getRelativeX for right side of frame`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 500f,
            top = 50f,
            right = 640f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        // centerX = (500 + 640) / 2 = 570
        // getRelativeX(640) = 570 / 640 = 0.89
        assertEquals(0.89f, obj.getRelativeX(640), 0.01f)
    }

    @Test
    fun `getDirection identifies LEFT correctly`() {
        // Object on LEFT side of frame (normalizedX < 0.33)
        val obj = DetectedObject(
            label = "car",
            labelIndex = 2,
            confidence = 0.85f,
            left = 0f,
            top = 100f,
            right = 150f,
            bottom = 400f,
            estimatedDistance = 5f
        )

        val direction = obj.getDirection(640, 480)
        assertEquals(DetectedObject.HorizontalDirection.LEFT, direction.horizontal)
    }

    @Test
    fun `getDirection identifies RIGHT correctly`() {
        // Object on RIGHT side of frame (normalizedX > 0.66)
        val obj = DetectedObject(
            label = "car",
            labelIndex = 2,
            confidence = 0.85f,
            left = 500f,
            top = 100f,
            right = 640f,
            bottom = 400f,
            estimatedDistance = 5f
        )

        val direction = obj.getDirection(640, 480)
        assertEquals(DetectedObject.HorizontalDirection.RIGHT, direction.horizontal)
    }

    @Test
    fun `getDirection identifies CENTER correctly`() {
        // Object in CENTER of frame
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 240f,
            top = 100f,
            right = 400f,
            bottom = 400f,
            estimatedDistance = 3f
        )

        val direction = obj.getDirection(640, 480)
        assertEquals(DetectedObject.HorizontalDirection.CENTER, direction.horizontal)
    }

    @Test
    fun `width and height are calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f,
            top = 50f,
            right = 300f,
            bottom = 350f,
            estimatedDistance = 3f
        )

        assertEquals(200f, obj.width, 0.01f)
        assertEquals(300f, obj.height, 0.01f)
    }

    @Test
    fun `area is calculated correctly`() {
        val obj = DetectedObject(
            label = "person",
            labelIndex = 0,
            confidence = 0.9f,
            left = 100f,
            top = 50f,
            right = 300f,
            bottom = 350f,
            estimatedDistance = 3f
        )

        // width = 200, height = 300, area = 200 * 300 = 60000
        assertEquals(60000f, obj.area, 0.01f)
    }
}
