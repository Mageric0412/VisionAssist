package com.visionassist.emergency

import android.content.Context
import timber.log.Timber

/**
 * Manager for emergency contacts
 * Provides CRUD operations and contact lookup for fall detection
 */
class EmergencyContactManager(context: Context) {

    private val database = EmergencyDatabase.getInstance(context)
    private val dao = database.emergencyContactDao()

    /**
     * Get all emergency contacts
     */
    fun getAllContacts(): List<EmergencyContact> {
        return try {
            dao.getAllContacts()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all contacts")
            emptyList()
        }
    }

    /**
     * Get the primary emergency contact
     * Returns null if no primary contact is set
     */
    fun getPrimaryContact(): EmergencyContact? {
        return try {
            dao.getPrimaryContact()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get primary contact")
            null
        }
    }

    /**
     * Get the best contact to call for fall alerts
     * Priority: Primary contact > First emergency number > First regular contact
     */
    fun getBestContactForAlert(): EmergencyContact? {
        return try {
            val contacts = dao.getAllContacts()
            when {
                contacts.isEmpty() -> null
                else -> {
                    // Try primary contact first
                    val primary = contacts.find { it.isPrimary }
                    if (primary != null) return primary

                    // Then try emergency numbers (120, 110, 119)
                    val emergency = contacts.find { it.isEmergencyNumber() }
                    if (emergency != null) return emergency

                    // Finally, just use the first contact
                    contacts.first()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get best contact for alert")
            null
        }
    }

    /**
     * Clear primary flag from all contacts except the specified one
     */
    private fun clearOtherPrimaryContacts(exceptId: Long? = null) {
        dao.getAllContacts()
            .filter { it.isPrimary && it.id != exceptId }
            .forEach { dao.update(it.copy(isPrimary = false)) }
    }

    /**
     * Add a new emergency contact
     * @return the ID of the inserted contact, or -1 on failure
     */
    fun addContact(name: String, phoneNumber: String, isPrimary: Boolean = false): Long {
        return try {
            // If setting as primary, unset other primary contacts first
            if (isPrimary) {
                clearOtherPrimaryContacts()
            }

            val contact = EmergencyContact(
                name = name,
                phoneNumber = phoneNumber,
                isPrimary = isPrimary
            )
            dao.insert(contact).also {
                Timber.d("Added contact: $name, id=$it")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add contact")
            -1
        }
    }

    /**
     * Update an existing contact
     */
    fun updateContact(contact: EmergencyContact): Boolean {
        return try {
            // If setting as primary, unset other primary contacts first
            if (contact.isPrimary) {
                clearOtherPrimaryContacts(exceptId = contact.id)
            }
            dao.update(contact)
            Timber.d("Updated contact: ${contact.name}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update contact")
            false
        }
    }

    /**
     * Delete a contact by ID
     */
    fun deleteContact(id: Long): Boolean {
        return try {
            dao.deleteById(id)
            Timber.d("Deleted contact id=$id")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete contact")
            false
        }
    }

    /**
     * Check if any emergency contacts exist
     */
    fun hasContacts(): Boolean {
        return try {
            dao.getContactCount() > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to check contacts")
            false
        }
    }

    /**
     * Get contact count
     */
    fun getContactCount(): Int {
        return try {
            dao.getContactCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get contact count")
            0
        }
    }
}
