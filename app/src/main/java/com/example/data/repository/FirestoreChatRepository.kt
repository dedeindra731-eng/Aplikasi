package com.example.data.repository

import android.util.Log
import com.example.data.model.FirestoreMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Production-ready Firestore Repository for real-time 1-on-1 messaging in Leuwijaya Chat.
 * Uses reactive Kotlin Flows backed by Firestore real-time snapshot listeners.
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreChatRepo"
        private const val COLLECTION_ROOMS = "rooms"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_TYPING = "typing"
    }

    /**
     * Observes real-time messages for a given chat room sorted chronologically.
     * Automatically registers and unregisters the Firestore snapshot listener with Flow lifecycle.
     */
    fun observeMessages(roomId: String): Flow<List<FirestoreMessage>> {
        val query = firestore.collection(COLLECTION_ROOMS)
            .document(roomId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreMessage::class.java)
            }
            .catch { exception ->
                Log.e(TAG, "Error observing messages for room: $roomId", exception)
                emit(emptyList())
            }
    }

    /**
     * Sends a new text message to Firestore with server-side timestamping.
     * Updates the room's lastMessage metadata atomically.
     */
    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        receiverId: String,
        content: String
    ): Result<String> {
        return try {
            val messagesRef = firestore.collection(COLLECTION_ROOMS)
                .document(roomId)
                .collection(COLLECTION_MESSAGES)

            val payload = hashMapOf(
                "chatRoomId" to roomId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "content" to content.trim(),
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "SENT",
                "type" to "TEXT"
            )

            val docRef = messagesRef.add(payload).await()

            // Update parent room metadata for chat preview
            val roomRef = firestore.collection(COLLECTION_ROOMS).document(roomId)
            roomRef.set(
                hashMapOf(
                    "lastMessage" to content.trim(),
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastSenderId" to senderId,
                    "participants" to listOf(senderId, receiverId)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to room $roomId", e)
            Result.failure(e)
        }
    }

    /**
     * Updates message delivery and read receipts status ("DELIVERED" or "READ").
     */
    suspend fun updateMessageStatus(
        roomId: String,
        messageId: String,
        newStatus: String
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_ROOMS)
                .document(roomId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status for message $messageId in room $roomId", e)
            Result.failure(e)
        }
    }

    /**
     * Broadcasts the user's typing indicator status to the room.
     */
    suspend fun setTypingStatus(
        roomId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_ROOMS)
                .document(roomId)
                .collection(COLLECTION_TYPING)
                .document(userId)
                .set(
                    hashMapOf(
                        "isTyping" to isTyping,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update typing status for user $userId in room $roomId", e)
            Result.failure(e)
        }
    }

    /**
     * Observes whether the other participant is currently typing in the room.
     */
    fun observeTypingStatus(roomId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        val docRef = firestore.collection(COLLECTION_ROOMS)
            .document(roomId)
            .collection(COLLECTION_TYPING)
            .document(otherUserId)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing typing status for $otherUserId", error)
                trySend(false)
                return@addSnapshotListener
            }
            val isTyping = snapshot?.getBoolean("isTyping") ?: false
            trySend(isTyping)
        }

        awaitClose { registration.remove() }
    }
}
