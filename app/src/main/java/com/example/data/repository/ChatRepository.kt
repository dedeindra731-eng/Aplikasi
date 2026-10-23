package com.example.data.repository

import com.example.data.model.ChatRoom
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

interface ChatRepository {
    val chatRooms: StateFlow<List<ChatRoom>>
    fun getMessagesForRoom(roomId: String): Flow<List<Message>>
    fun getTypingState(roomId: String): Flow<Boolean>
    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        receiverId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUri: String? = null,
        fileName: String? = null,
        fileSizeFormatted: String? = null,
        durationSeconds: Int = 0,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ): Message
    suspend fun setTyping(roomId: String, isTyping: Boolean)
    suspend fun getOrCreateRoom(currentUserId: String, otherUser: User): ChatRoom
    suspend fun markRoomAsRead(roomId: String)
}

class InMemoryChatRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ChatRepository {

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    override val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        // Seed default initial chat room and messages
        val initialUser = User(
            uid = "usr_siti_002",
            displayName = "Siti Nurhaliza",
            email = "siti@leuwijaya.com",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
            barcodeId = "LJ-UID-usr_siti_002",
            bio = "Mobile UX Designer 🎨",
            isOnline = true
        )
        val roomId = "room_indra_siti"
        val sampleMsgs = listOf(
            Message(
                id = "m1",
                chatRoomId = roomId,
                senderId = "usr_siti_002",
                receiverId = "usr_indra_001",
                content = "Halo Kang Indra! Bagaimana progres arsitektur Leuwijaya Chat?",
                timestamp = System.currentTimeMillis() - 3600000,
                status = MessageStatus.READ
            ),
            Message(
                id = "m2",
                chatRoomId = roomId,
                senderId = "usr_indra_001",
                receiverId = "usr_siti_002",
                content = "Halo Siti! Sangat mantap, fitur ML Kit Barcode Scanner dan WebRTC video call sudah terintegrasi rapi dengan Clean Architecture.",
                timestamp = System.currentTimeMillis() - 1800000,
                status = MessageStatus.READ
            ),
            Message(
                id = "m3",
                chatRoomId = roomId,
                senderId = "usr_siti_002",
                receiverId = "usr_indra_001",
                content = "Keren banget! Coba kita tes panggilan video nanti yaa 📹",
                timestamp = System.currentTimeMillis() - 300000,
                status = MessageStatus.READ
            )
        )

        _messages.value = mapOf(roomId to sampleMsgs)
        _chatRooms.value = listOf(
            ChatRoom(
                id = roomId,
                participantIds = listOf("usr_indra_001", "usr_siti_002"),
                otherUser = initialUser,
                lastMessage = sampleMsgs.last().content,
                lastMessageTimestamp = sampleMsgs.last().timestamp,
                unreadCount = 0
            )
        )
    }

    override fun getMessagesForRoom(roomId: String): Flow<List<Message>> {
        return _messages.map { map -> map[roomId] ?: emptyList() }
    }

    override fun getTypingState(roomId: String): Flow<Boolean> {
        return _typingStates.map { it[roomId] ?: false }
    }

    override suspend fun sendMessage(
        roomId: String,
        senderId: String,
        receiverId: String,
        content: String,
        type: MessageType,
        mediaUri: String?,
        fileName: String?,
        fileSizeFormatted: String?,
        durationSeconds: Int,
        latitude: Double?,
        longitude: Double?,
        locationName: String?
    ): Message {
        val newMsg = Message(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            chatRoomId = roomId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            type = type,
            mediaUri = mediaUri,
            fileName = fileName,
            fileSizeFormatted = fileSizeFormatted,
            durationSeconds = durationSeconds,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName
        )

        val currentList = _messages.value[roomId] ?: emptyList()
        _messages.value = _messages.value + (roomId to (currentList + newMsg))

        // Update chat room header
        val previewText = when (type) {
            MessageType.IMAGE -> "📷 Foto"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.FILE -> "📄 Dokumen: ${fileName ?: "Berkas"}"
            MessageType.VOICE_NOTE -> "🎤 Pesan Suara (${durationSeconds}d)"
            MessageType.LOCATION -> "📍 Lokasi: ${locationName ?: "Koordinat GPS"}"
            else -> content
        }

        _chatRooms.value = _chatRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(
                    lastMessage = previewText,
                    lastMessageTimestamp = newMsg.timestamp
                )
            } else room
        }

        // Simulate delivery and auto-reply for realistic interactive experience
        scope.launch {
            delay(1000)
            updateMessageStatus(roomId, newMsg.id, MessageStatus.DELIVERED)
            delay(1500)
            updateMessageStatus(roomId, newMsg.id, MessageStatus.READ)

            // Auto reply acknowledging the specific attachment type
            delay(1200)
            _typingStates.value = _typingStates.value + (roomId to true)
            delay(1800)
            _typingStates.value = _typingStates.value + (roomId to false)

            val replyContent = when (type) {
                MessageType.IMAGE -> "Fotonya jernih sekali! Terima kasih sudah berbagi 🖼️"
                MessageType.VIDEO -> "Videonya sudah saya tonton, kualitasnya mantap! 🎥"
                MessageType.FILE -> "Berkas ${fileName ?: "dokumen"} sudah saya terima dan simpan 📂"
                MessageType.VOICE_NOTE -> "Pesan suaramu terdengar sangat jelas! 🎧"
                MessageType.LOCATION -> "Lokasi diterima (${locationName ?: "GPS"}), terima kasih atas petunjuknya! 📍"
                else -> "Pesan diterima: \"$content\". Siap untuk video call / barcode test kapan saja! 👍"
            }

            val autoReply = Message(
                id = "msg_" + UUID.randomUUID().toString().take(8),
                chatRoomId = roomId,
                senderId = receiverId,
                receiverId = senderId,
                content = replyContent,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                type = MessageType.TEXT
            )
            val updatedList = _messages.value[roomId] ?: emptyList()
            _messages.value = _messages.value + (roomId to (updatedList + autoReply))
            _chatRooms.value = _chatRooms.value.map { room ->
                if (room.id == roomId) {
                    room.copy(
                        lastMessage = autoReply.content,
                        lastMessageTimestamp = autoReply.timestamp
                    )
                } else room
            }
        }

        return newMsg
    }

    private fun updateMessageStatus(roomId: String, msgId: String, newStatus: MessageStatus) {
        val list = _messages.value[roomId] ?: return
        _messages.value = _messages.value + (roomId to list.map {
            if (it.id == msgId) it.copy(status = newStatus) else it
        })
    }

    override suspend fun setTyping(roomId: String, isTyping: Boolean) {
        _typingStates.value = _typingStates.value + (roomId to isTyping)
    }

    override suspend fun getOrCreateRoom(currentUserId: String, otherUser: User): ChatRoom {
        val existing = _chatRooms.value.find { room ->
            room.otherUser.uid == otherUser.uid || 
            (room.participantIds.contains(currentUserId) && room.participantIds.contains(otherUser.uid))
        }
        if (existing != null) return existing

        val newRoomId = "room_${currentUserId.take(5)}_${otherUser.uid.take(5)}"
        val newRoom = ChatRoom(
            id = newRoomId,
            participantIds = listOf(currentUserId, otherUser.uid),
            otherUser = otherUser,
            lastMessage = "Mulai obrolan baru...",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        _chatRooms.value = listOf(newRoom) + _chatRooms.value
        return newRoom
    }

    override suspend fun markRoomAsRead(roomId: String) {
        _chatRooms.value = _chatRooms.value.map {
            if (it.id == roomId) it.copy(unreadCount = 0) else it
        }
    }
}
