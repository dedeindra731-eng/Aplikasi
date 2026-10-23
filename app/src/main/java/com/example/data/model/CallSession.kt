package com.example.data.model

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    IDLE,
    DIALING,       // Caller waiting for response
    RINGING,       // Callee receiving incoming call
    CONNECTING,    // WebRTC peer connection negotiating SDP/ICE
    CONNECTED,     // Active call established
    ENDED,         // Hang up normally
    REJECTED,      // Callee declined
    BUSY,          // Callee unavailable
    FAILED         // Connection timeout or error
}

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val callType: CallType = CallType.VIDEO,
    val status: CallStatus = CallStatus.IDLE,
    val startedAt: Long? = null,
    val durationSeconds: Int = 0,
    val isMicMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeakerOn: Boolean = true
)
