package com.visionassist.places

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PlaceOfInterest data class
 */
class PlaceOfInterestTest {

    @Test
    fun `isEmergencyPlace returns true for home keywords`() {
        val places = listOf(
            PlaceOfInterest(id = 1, name = "家", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 2, name = "Home", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 3, name = "home", latitude = 0.0, longitude = 0.0, reminderText = "提醒")
        )

        for (place in places) {
            assertTrue("Place '${place.name}' should be emergency", place.isEmergencyPlace())
        }
    }

    @Test
    fun `isEmergencyPlace returns true for work keywords`() {
        val places = listOf(
            PlaceOfInterest(id = 1, name = "工作", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 2, name = "Work", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 3, name = "我的工作", latitude = 0.0, longitude = 0.0, reminderText = "提醒")
        )

        for (place in places) {
            assertTrue("Place '${place.name}' should be emergency", place.isEmergencyPlace())
        }
    }

    @Test
    fun `isEmergencyPlace returns true for parents keywords`() {
        val place = PlaceOfInterest(
            id = 1,
            name = "父母家",
            latitude = 0.0,
            longitude = 0.0,
            reminderText = "提醒"
        )

        assertTrue("Place '${place.name}' should be emergency", place.isEmergencyPlace())
    }

    @Test
    fun `isEmergencyPlace returns false for regular places`() {
        val places = listOf(
            PlaceOfInterest(id = 1, name = "咖啡店", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 2, name = "Restaurant", latitude = 0.0, longitude = 0.0, reminderText = "提醒"),
            PlaceOfInterest(id = 3, name = "公园", latitude = 0.0, longitude = 0.0, reminderText = "提醒")
        )

        for (place in places) {
            assertFalse("Place '${place.name}' should not be emergency", place.isEmergencyPlace())
        }
    }

    @Test
    fun `place has correct default values`() {
        val place = PlaceOfInterest(
            name = "Test Place",
            latitude = 31.2304,
            longitude = 121.4737,
            reminderText = "Test reminder"
        )

        assertEquals(0L, place.id)
        assertEquals("Test Place", place.name)
        assertEquals(31.2304, place.latitude, 0.0001)
        assertEquals(121.4737, place.longitude, 0.0001)
        assertEquals(20f, place.radiusMeters, 0.01f)
        assertEquals("Test reminder", place.reminderText)
        assertTrue(place.isActive)
        assertNull(place.lastTriggered)
        assertTrue(place.createdAt > 0)
    }

    @Test
    fun `place can store last triggered time`() {
        val triggerTime = System.currentTimeMillis()
        val place = PlaceOfInterest(
            id = 1,
            name = "Test",
            latitude = 0.0,
            longitude = 0.0,
            reminderText = "提醒",
            lastTriggered = triggerTime
        )

        assertEquals(triggerTime, place.lastTriggered)
    }
}
