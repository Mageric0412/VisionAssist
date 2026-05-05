package com.visionassist.emergency

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EmergencyContactManager
 * Tests contact priority logic and CRUD operations
 */
class EmergencyContactManagerTest {

    /**
     * Test emergency contact data class
     */
    @Test
    fun `emergency contact has correct properties`() {
        val contact = EmergencyContact(
            id = 1,
            name = "John Doe",
            phoneNumber = "1234567890",
            isPrimary = true
        )

        assertEquals(1L, contact.id)
        assertEquals("John Doe", contact.name)
        assertEquals("1234567890", contact.phoneNumber)
        assertTrue(contact.isPrimary)
    }

    /**
     * Test isEmergencyNumber detection
     */
    @Test
    fun `isEmergencyNumber detects emergency numbers`() {
        val emergencyNumbers = listOf("120", "110", "119", "112")

        emergencyNumbers.forEach { number ->
            val contact = EmergencyContact(
                id = 1,
                name = "Emergency",
                phoneNumber = number,
                isPrimary = false
            )
            assertTrue("'$number' should be emergency number", contact.isEmergencyNumber())
        }
    }

    @Test
    fun `isEmergencyNumber returns false for regular numbers`() {
        val regularNumbers = listOf("1234567890", "555-1234", "000", "911")

        regularNumbers.forEach { number ->
            val contact = EmergencyContact(
                id = 1,
                name = "Regular",
                phoneNumber = number,
                isPrimary = false
            )
            assertFalse("'$number' should not be emergency number", contact.isEmergencyNumber())
        }
    }

    /**
     * Test contact defaults
     */
    @Test
    fun `contact defaults to non-primary`() {
        val contact = EmergencyContact(
            name = "Test",
            phoneNumber = "123"
        )

        assertFalse(contact.isPrimary)
    }

    /**
     * Test emergency contact copy with updated fields
     */
    @Test
    fun `contact copy updates fields correctly`() {
        val original = EmergencyContact(
            id = 1,
            name = "Original",
            phoneNumber = "111",
            isPrimary = true
        )

        val updated = original.copy(isPrimary = false)

        assertEquals(1L, updated.id)
        assertEquals("Original", updated.name)
        assertEquals("111", updated.phoneNumber)
        assertFalse(updated.isPrimary)

        // Original unchanged
        assertTrue(original.isPrimary)
    }

    /**
     * Test contact equality
     */
    @Test
    fun `contacts with same values are equal`() {
        val contact1 = EmergencyContact(
            id = 1,
            name = "Test",
            phoneNumber = "123",
            isPrimary = false
        )

        val contact2 = EmergencyContact(
            id = 1,
            name = "Test",
            phoneNumber = "123",
            isPrimary = false
        )

        assertEquals(contact1, contact2)
    }

    /**
     * Test contact with different ids are not equal
     */
    @Test
    fun `contacts with different ids are not equal`() {
        val contact1 = EmergencyContact(
            id = 1,
            name = "Test",
            phoneNumber = "123",
            isPrimary = false
        )

        val contact2 = EmergencyContact(
            id = 2,
            name = "Test",
            phoneNumber = "123",
            isPrimary = false
        )

        assertNotEquals(contact1, contact2)
    }
}

/**
 * Unit tests for EmergencyContact copy behavior
 */
class EmergencyContactCopyTest {

    @Test
    fun `copy preserves id when updating name`() {
        val original = EmergencyContact(
            id = 5,
            name = "Original",
            phoneNumber = "111",
            isPrimary = false
        )

        val copy = original.copy(name = "Updated")
        assertEquals(5L, copy.id)
        assertEquals("Updated", copy.name)
        assertEquals("111", copy.phoneNumber)
    }

    @Test
    fun `copy can change primary status`() {
        val original = EmergencyContact(
            id = 1,
            name = "Primary",
            phoneNumber = "111",
            isPrimary = true
        )

        val demoted = original.copy(isPrimary = false)
        assertTrue(original.isPrimary)
        assertFalse(demoted.isPrimary)
    }
}
