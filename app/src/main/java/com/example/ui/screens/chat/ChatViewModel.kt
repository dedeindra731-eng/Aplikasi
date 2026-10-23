package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Message
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val currentRoomId: String = "",
    val otherUser: User = User(),
    val inputText: String = "",
    val isSending: Boolean = false
)

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private val _activeRoomId = MutableStateFlow("")

    val messages: StateFlow<List<Message>> = _activeRoomId.flatMapLatest { roomId ->
        chatRepository.getMessagesForRoom(roomId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isOtherUserTyping: StateFlow<Boolean> = _activeRoomId.flatMapLatest { roomId ->
        chatRepository.getTypingState(roomId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setRoom(roomId: String, otherUser: User) {
        _activeRoomId.value = roomId
        _uiState.value = _uiState.value.copy(
            currentRoomId = roomId,
            otherUser = otherUser
        )
        viewModelScope.launch {
            chatRepository.markRoomAsRead(roomId)
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
        viewModelScope.launch {
            val roomId = _uiState.value.currentRoomId
            if (roomId.isNotBlank()) {
                chatRepository.setTyping(roomId, text.isNotBlank())
            }
        }
    }

    fun sendMessage(senderId: String) {
        val text = _uiState.value.inputText.trim()
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid

        if (text.isBlank() || roomId.isBlank()) return

        _uiState.value = _uiState.value.copy(inputText = "")
        viewModelScope.launch {
            chatRepository.setTyping(roomId, false)
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = text
            )
        }
    }

    fun sendImage(senderId: String, imageUri: String) {
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid
        if (roomId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = "Gambar",
                type = com.example.data.model.MessageType.IMAGE,
                mediaUri = imageUri
            )
        }
    }

    fun sendVideo(senderId: String, videoUri: String) {
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid
        if (roomId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = "Video",
                type = com.example.data.model.MessageType.VIDEO,
                mediaUri = videoUri
            )
        }
    }

    fun sendFile(senderId: String, fileUri: String, fileName: String, fileSize: String) {
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid
        if (roomId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = fileName,
                type = com.example.data.model.MessageType.FILE,
                mediaUri = fileUri,
                fileName = fileName,
                fileSizeFormatted = fileSize
            )
        }
    }

    fun sendVoiceNote(senderId: String, audioUri: String, durationSecs: Int) {
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid
        if (roomId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = "Pesan Suara",
                type = com.example.data.model.MessageType.VOICE_NOTE,
                mediaUri = audioUri,
                durationSeconds = durationSecs
            )
        }
    }

    fun sendLocation(senderId: String, lat: Double, lng: Double, locationName: String) {
        val roomId = _uiState.value.currentRoomId
        val receiverId = _uiState.value.otherUser.uid
        if (roomId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                senderId = senderId,
                receiverId = receiverId,
                content = locationName,
                type = com.example.data.model.MessageType.LOCATION,
                latitude = lat,
                longitude = lng,
                locationName = locationName
            )
        }
    }
}
