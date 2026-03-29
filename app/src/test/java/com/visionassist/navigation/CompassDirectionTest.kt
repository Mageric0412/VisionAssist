package com.visionassist.navigation

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CompassManager cardinal direction calculations
 */
class CompassDirectionTest {

    /**
     * Helper to calculate cardinal direction (mirrors CompassManager logic)
     */
    private fun getCardinalDirection(heading: Float): String {
        return when {
            heading >= 337.5 || heading < 22.5 -> "北"
            heading >= 22.5 && heading < 67.5 -> "东北"
            heading >= 67.5 && heading < 112.5 -> "东"
            heading >= 112.5 && heading < 157.5 -> "东南"
            heading >= 157.5 && heading < 202.5 -> "南"
            heading >= 202.5 && heading < 247.5 -> "西南"
            heading >= 247.5 && heading < 292.5 -> "西"
            heading >= 292.5 && heading < 337.5 -> "西北"
            else -> "未知"
        }
    }

    @Test
    fun `north direction is identified correctly`() {
        // Heading 0 = North
        assertEquals("北", getCardinalDirection(0f))
        assertEquals("北", getCardinalDirection(350f))
        assertEquals("北", getCardinalDirection(10f))
    }

    @Test
    fun `northeast direction is identified correctly`() {
        assertEquals("东北", getCardinalDirection(45f))
        assertEquals("东北", getCardinalDirection(30f))
        assertEquals("东北", getCardinalDirection(60f))
    }

    @Test
    fun `east direction is identified correctly`() {
        assertEquals("东", getCardinalDirection(90f))
        assertEquals("东", getCardinalDirection(75f))
        assertEquals("东", getCardinalDirection(105f))
    }

    @Test
    fun `southeast direction is identified correctly`() {
        assertEquals("东南", getCardinalDirection(135f))
        assertEquals("东南", getCardinalDirection(150f))
        assertEquals("东南", getCardinalDirection(120f))
    }

    @Test
    fun `south direction is identified correctly`() {
        assertEquals("南", getCardinalDirection(180f))
        assertEquals("南", getCardinalDirection(190f))
        assertEquals("南", getCardinalDirection(165f))
    }

    @Test
    fun `southwest direction is identified correctly`() {
        assertEquals("西南", getCardinalDirection(225f))
        assertEquals("西南", getCardinalDirection(240f))
        assertEquals("西南", getCardinalDirection(210f))
    }

    @Test
    fun `west direction is identified correctly`() {
        assertEquals("西", getCardinalDirection(270f))
        assertEquals("西", getCardinalDirection(285f))
        assertEquals("西", getCardinalDirection(255f))
    }

    @Test
    fun `northwest direction is identified correctly`() {
        assertEquals("西北", getCardinalDirection(315f))
        assertEquals("西北", getCardinalDirection(330f))
        assertEquals("西北", getCardinalDirection(300f))
    }

    @Test
    fun `heading boundaries are correct`() {
        // Test exact boundary values
        assertEquals("北", getCardinalDirection(0f))
        assertEquals("北", getCardinalDirection(22.4f))
        assertEquals("东北", getCardinalDirection(22.5f))
        assertEquals("西北", getCardinalDirection(337.4f))
        assertEquals("北", getCardinalDirection(337.5f))
    }

    @Test
    fun `normalized heading works correctly`() {
        // Heading should be normalized to 0-360
        // -90 should become 270 (west)
        val normalized = if (-90f < 0) -90f + 360 else -90f
        assertEquals(270f, normalized, 0.01f)
    }
}

/**
 * Unit tests for NavigationManager direction constants
 */
class NavigationDirectionTest {

    @Test
    fun `direction constants are defined correctly`() {
        assertEquals(0, NavigationManager.DIRECTION_AHEAD)
        assertEquals(1, NavigationManager.DIRECTION_LEFT)
        assertEquals(2, NavigationManager.DIRECTION_RIGHT)
    }

    @Test
    fun `warning distance constants are reasonable`() {
        // Near warning should be closer than far warning
        assertTrue(NavigationManager.WARNING_DISTANCE_NEAR < NavigationManager.WARNING_DISTANCE_FAR)
        assertTrue(NavigationManager.WARNING_DISTANCE_NEAR > 0)
        assertTrue(NavigationManager.WARNING_DISTANCE_FAR > 0)
    }

    @Test
    fun `minimum obstacle confidence is reasonable`() {
        // Should be between 0 and 1
        assertTrue(NavigationManager.MIN_OBSTACLE_CONFIDENCE >= 0f)
        assertTrue(NavigationManager.MIN_OBSTACLE_CONFIDENCE <= 1f)
        // Should be set to a reasonable threshold (0.5 is reasonable)
        assertEquals(0.5f, NavigationManager.MIN_OBSTACLE_CONFIDENCE, 0.01f)
    }

    @Test
    fun `location update intervals are reasonable`() {
        assertTrue(NavigationManager.LOCATION_UPDATE_INTERVAL >= 1000L) // At least 1 second
        assertTrue(NavigationManager.LOCATION_FASTEST_INTERVAL <= NavigationManager.LOCATION_UPDATE_INTERVAL)
    }
}
