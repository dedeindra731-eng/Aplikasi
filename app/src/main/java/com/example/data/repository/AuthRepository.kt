package com.example.data.repository

import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun loginWithEmail(email: String, password: String):Result<User>
    suspend fun registerWithEmail(name: String, email: String, password: String, phone: String): Result<User>
    suspend fun loginWithPhone(phone: String, code: String): Result<User>
    suspend fun loginAsDemoUser(demoIndex: Int): Result<User>
    suspend fun updateProfile(name: String, bio: String): Result<User>
    suspend fun updateAvatar(avatarUrl: String): Result<User>
    suspend fun logout()
}

class InMemoryAuthRepository : AuthRepository {
    private val demoUsers = listOf(
        User(
            uid = "usr_indra_001",
            displayName = "Dede Indra",
            email = "dedeindra@leuwijaya.com",
            phoneNumber = "+6281234567890",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
            barcodeId = "LJ-UID-usr_indra_001",
            bio = "Founder & Architect at Leuwijaya Tech 🚀",
            isOnline = true
        ),
        User(
            uid = "usr_siti_002",
            displayName = "Siti Nurhaliza",
            email = "siti@leuwijaya.com",
            phoneNumber = "+6281298765432",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
            barcodeId = "LJ-UID-usr_siti_002",
            bio = "Mobile UX Enthusiast & Designer 🎨",
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
        )
    )

    private val _currentUser = MutableStateFlow<User?>(demoUsers[0])
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        val matched = demoUsers.find { it.email.equals(email, ignoreCase = true) }
        val user = matched ?: User(
            uid = "usr_" + UUID.randomUUID().toString().take(8),
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            phoneNumber = "+62812" + (10000000..99999999).random(),
            barcodeId = "LJ-UID-" + UUID.randomUUID().toString().take(8),
            bio = "Active Leuwijaya Chat user"
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<User> {
        val uniqueId = UUID.randomUUID().toString().take(8)
        val newUser = User(
            uid = "usr_$uniqueId",
            displayName = name,
            email = email,
            phoneNumber = phone.ifBlank { "+6281200000" },
            barcodeId = "LJ-UID-$uniqueId",
            bio = "Ready to connect on Leuwijaya Chat!"
        )
        _currentUser.value = newUser
        return Result.success(newUser)
    }

    override suspend fun loginWithPhone(phone: String, code: String): Result<User> {
        val matched = demoUsers.find { it.phoneNumber == phone }
        val user = matched ?: User(
            uid = "usr_" + UUID.randomUUID().toString().take(8),
            displayName = "User " + phone.takeLast(4),
            email = "user${phone.takeLast(4)}@leuwijaya.com",
            phoneNumber = phone,
            barcodeId = "LJ-UID-" + UUID.randomUUID().toString().take(8),
            bio = "Connected via Phone"
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun loginAsDemoUser(demoIndex: Int): Result<User> {
        val user = demoUsers.getOrElse(demoIndex) { demoUsers[0] }
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun updateProfile(name: String, bio: String): Result<User> {
        val current = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val updated = current.copy(displayName = name, bio = bio)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun updateAvatar(avatarUrl: String): Result<User> {
        val current = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val updated = current.copy(avatarUrl = avatarUrl)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }
}
