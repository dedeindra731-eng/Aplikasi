package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Type-safe Cloud Firestore Data Model for 1-on-1 and group chat messages.
 * Includes @DocumentId for automatic ID binding and @ServerTimestamp for cloud consistency.
 */
@IgnoreExtraProperties
data class FirestoreMessage(
    @DocumentId val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val status: String = "SENT", // "SENT", "DELIVERED", "READ"
    val type: String = "TEXT"
)
