package com.example.data.repository

import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

interface ContactRepository {
    val contacts: StateFlow<List<User>>
    suspend fun addContactByBarcode(scannedRawData: String): Result<User>
    suspend fun addContact(user: User): Result<Unit>
    suspend fun getContactByUid(uid: String): User?
    suspend fun removeContact(uid: String)
}

class InMemoryContactRepository : ContactRepository {
    private val directory = listOf(
        User(
            uid = "usr_siti_002",
            displayName = "Siti Nurhaliza",
            email = "siti@leuwijaya.com",
            phoneNumber = "+6281298765432",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
            barcodeId = "LJ-UID-usr_siti_002",
            bio = "Mobile UX Designer 🎨",
            isOnline = true
        ),
        User(
            uid = "usr_budi_003",
            displayName = "Budi Pratama",
            email = "budi@leuwijaya.com",
            phoneNumber = "+6281311223344",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
            barcodeId = "LJ-UID-usr_budi_003",
            bio = "Android & Kotlin enthusiast ☕",
            isOnline = false
        ),
        User(
            uid = "usr_anita_004",
            displayName = "Anita Wijaya",
            email = "anita@leuwijaya.com",
            phoneNumber = "+6281899887766",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9",
            barcodeId = "LJ-UID-usr_anita_004",
            bio = "DevOps & Cloud Engineer ☁️",
            isOnline = true
        ),
        User(
            uid = "usr_fadli_005",
            displayName = "Fadli Rahman",
            email = "fadli@leuwijaya.com",
            phoneNumber = "+6281733445566",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e",
            barcodeId = "LJ-UID-usr_fadli_005",
            bio = "WebRTC & Video Streaming Geek 📹",
            isOnline = true
        )
    )

    private val _contacts = MutableStateFlow<List<User>>(
        listOf(directory[0], directory[1])
    )
    override val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    override suspend fun addContactByBarcode(scannedRawData: String): Result<User> {
        val trimmed = scannedRawData.trim()
        
        // Extract UID from barcode ID format "LJ-UID-<id>", JSON, or plain text
        val targetUid = when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                try {
                    val json = JSONObject(trimmed)
                    json.optString("uid", json.optString("id", trimmed))
                } catch (e: Exception) {
                    trimmed
                }
            }
            trimmed.startsWith("LJ-UID-") -> {
                trimmed.removePrefix("LJ-UID-")
            }
            trimmed.contains("leuwijayachat.app/user/") -> {
                trimmed.substringAfterLast("/")
            }
            else -> trimmed
        }

        // Check if already a contact
        val existing = _contacts.value.find { it.uid == targetUid || it.barcodeId == trimmed }
        if (existing != null) {
            return Result.success(existing)
        }

        // Look up in directory or create new contact dynamically
        val foundInDirectory = directory.find { 
            it.uid == targetUid || it.barcodeId == trimmed || it.uid == "usr_$targetUid"
        }

        val newContact = foundInDirectory ?: User(
            uid = if (targetUid.startsWith("usr_")) targetUid else "usr_$targetUid",
            displayName = "Teman Scanned (${targetUid.take(6)})",
            email = "$targetUid@leuwijaya.com",
            phoneNumber = "+62812" + (10000000..99999999).random(),
            barcodeId = if (trimmed.startsWith("LJ-UID-")) trimmed else "LJ-UID-$targetUid",
            bio = "Ditambahkan via Pemindaian Barcode ML Kit",
            isOnline = true
        )

        _contacts.value = _contacts.value + newContact
        return Result.success(newContact)
    }

    override suspend fun addContact(user: User): Result<Unit> {
        if (_contacts.value.none { it.uid == user.uid }) {
            _contacts.value = _contacts.value + user
        }
        return Result.success(Unit)
    }

    override suspend fun getContactByUid(uid: String): User? {
        return _contacts.value.find { it.uid == uid } 
            ?: directory.find { it.uid == uid }
    }

    override suspend fun removeContact(uid: String) {
        _contacts.value = _contacts.value.filter { it.uid != uid }
    }
}
