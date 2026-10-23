package com.example.data.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val barcodeId: String = "",
    val bio: String = "Hey there! I am using Leuwijaya Chat.",
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
