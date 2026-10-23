package com.example.data.model

data class ChatRoom(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val otherUser: User = User(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOtherUserTyping: Boolean = false
)
