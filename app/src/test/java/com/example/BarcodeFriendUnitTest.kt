package com.example

import com.example.data.repository.InMemoryAuthRepository
import com.example.data.repository.InMemoryContactRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeFriendUnitTest {

    @Test
    fun testAddContactByBarcodeId() = runBlocking {
        val contactRepo = InMemoryContactRepository()
        
        // Scan Anita's barcode
        val result = contactRepo.addContactByBarcode("LJ-UID-usr_anita_004")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("Anita Wijaya", user?.displayName)
        
        // Confirm contact list contains Anita
        val contacts = contactRepo.contacts.value
        assertTrue(contacts.any { it.uid == "usr_anita_004" })
    }

    @Test
    fun testAuthCreatesUniqueBarcodeId() = runBlocking {
        val authRepo = InMemoryAuthRepository()
        val regResult = authRepo.registerWithEmail(
            name = "Test User",
            email = "test@leuwijaya.com",
            password = "pass",
            phone = "+62812333"
        )
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrNull()
        assertNotNull(user)
        assertTrue(user!!.barcodeId.startsWith("LJ-UID-"))
    }

    @Test
    fun testUpdateAvatar() = runBlocking {
        val authRepo = InMemoryAuthRepository()
        val newAvatar = "content://media/external/images/media/42"
        val result = authRepo.updateAvatar(newAvatar)
        assertTrue(result.isSuccess)
        assertEquals(newAvatar, authRepo.currentUser.value?.avatarUrl)
    }
}

