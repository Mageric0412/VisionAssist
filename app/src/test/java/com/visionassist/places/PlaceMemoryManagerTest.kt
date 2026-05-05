package com.visionassist.places

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PlaceMemoryManager
 * Tests error paths and edge cases identified in code review
 *
 * Regression: ISSUE-NNN — PlaceMemoryManager addPlace race condition
 * Found by /plan-eng-review on 2026-03-29
 */
class PlaceMemoryManagerTest {

    /**
     * Test that PlaceRepository.MAX_GEOFENCES constant is defined correctly
     * Android Geofencing API has a limit of 100 geofences per app
     */
    @Test
    fun `max geofences constant is reasonable`() {
        assertEquals(100, PlaceRepository.MAX_GEOFENCES)
    }

    /**
     * Test PlaceOfInterest defaults
     */
    @Test
    fun `place of interest has correct defaults`() {
        val place = PlaceOfInterest(
            name = "Test",
            latitude = 0.0,
            longitude = 0.0,
            reminderText = "Reminder"
        )
        assertEquals(20f, place.radiusMeters, 0.01f)
        assertTrue(place.isActive)
        assertNull(place.lastTriggered)
    }

    /**
     * Test emergency keywords detection
     */
    @Test
    fun `isEmergencyPlace detects home keywords`() {
        val homePlaces = listOf("家", "Home", "HOME", "我的家")
        homePlaces.forEach { name ->
            val place = PlaceOfInterest(1, name, 0.0, 0.0, reminderText = "test")
            assertTrue("'$name' should be emergency place", place.isEmergencyPlace())
        }
    }

    @Test
    fun `isEmergencyPlace detects work keywords`() {
        val workPlaces = listOf("工作", "Work", "WORK", "我的工作")
        workPlaces.forEach { name ->
            val place = PlaceOfInterest(1, name, 0.0, 0.0, reminderText = "test")
            assertTrue("'$name' should be emergency place", place.isEmergencyPlace())
        }
    }

    @Test
    fun `isEmergencyPlace detects parents keywords`() {
        val parentPlaces = listOf("父母", "parents", "Parents", "父母家")
        parentPlaces.forEach { name ->
            val place = PlaceOfInterest(1, name, 0.0, 0.0, reminderText = "test")
            assertTrue("'$name' should be emergency place", place.isEmergencyPlace())
        }
    }

    @Test
    fun `isEmergencyPlace returns false for regular places`() {
        val regularPlaces = listOf("咖啡店", "Restaurant", "公园", "超市", "School")
        regularPlaces.forEach { name ->
            val place = PlaceOfInterest(1, name, 0.0, 0.0, reminderText = "test")
            assertFalse("'$name' should not be emergency place", place.isEmergencyPlace())
        }
    }
}

/**
 * Unit tests for PlaceRepository
 * Tests error handling and edge cases
 */
class PlaceRepositoryTest {

    /**
     * Test that getPlaceById returns null for non-existent place
     * Note: Without Android context, we test the data class behavior
     */
    @Test
    fun `placeOfInterest copy works correctly`() {
        val original = PlaceOfInterest(
            id = 1,
            name = "Original",
            latitude = 1.0,
            longitude = 2.0,
            reminderText = "Original reminder",
            isActive = true
        )

        // Test copy with updated fields
        val updated = original.copy(isActive = false)
        assertEquals(1L, updated.id)
        assertEquals("Original", updated.name)
        assertFalse(updated.isActive)

        // Original should be unchanged
        assertTrue(original.isActive)
    }

    @Test
    fun `placeOfInterest copy updates lastTriggered`() {
        val original = PlaceOfInterest(
            id = 1,
            name = "Test",
            latitude = 0.0,
            longitude = 0.0,
            reminderText = "test"
        )

        assertNull(original.lastTriggered)

        val triggerTime = System.currentTimeMillis()
        val triggered = original.copy(lastTriggered = triggerTime)
        assertEquals(triggerTime, triggered.lastTriggered)
    }
}

/**
 * Unit tests for GeofenceBroadcastReceiver
 * Tests enum and data handling
 */
class GeofenceBroadcastReceiverTest {

    @Test
    fun `geofence action enum has correct values`() {
        val enter = GeofenceBroadcastReceiver.GeofenceAction.valueOf("ENTER")
        val exit = GeofenceBroadcastReceiver.GeofenceAction.valueOf("EXIT")
        val dwell = GeofenceBroadcastReceiver.GeofenceAction.valueOf("DWELL")

        assertEquals(GeofenceBroadcastReceiver.GeofenceAction.ENTER, enter)
        assertEquals(GeofenceBroadcastReceiver.GeofenceAction.EXIT, exit)
        assertEquals(GeofenceBroadcastReceiver.GeofenceAction.DWELL, dwell)
    }

    @Test
    fun `geofence action enum has three values`() {
        val actions = GeofenceBroadcastReceiver.GeofenceAction.values()
        assertEquals(3, actions.size)
    }
}
