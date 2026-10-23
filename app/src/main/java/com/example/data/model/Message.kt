package com.example.data.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    VOICE_NOTE,
    LOCATION,
    CALL_LOG
}

data class Message(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val type: MessageType = MessageType.TEXT,
    // Media & Location payloads
    val mediaUri: String? = null,
    val fileName: String? = null,
    val fileSizeFormatted: String? = null,
    val durationSeconds: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)
