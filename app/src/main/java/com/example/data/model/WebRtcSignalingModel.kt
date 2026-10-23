package com.example.data.model

/**
 * WebRTC Signaling Data Models used for SDP Offer, Answer, and ICE Candidate exchanges
 * between peers via Cloud Firestore or WebSocket signaling channels.
 */
data class IceCandidateModel(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val serverUrl: String = ""
)

data class SessionDescriptionModel(
    val type: String = "", // "offer" or "answer"
    val description: String = ""
)

data class SignalingMessage(
    val callId: String = "",
    val senderId: String = "",
    val targetId: String = "",
    val type: SignalingMessageType = SignalingMessageType.OFFER,
    val sdp: String? = null,
    val iceCandidate: IceCandidateModel? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalingMessageType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    HANGUP,
    RINGING,
    REJECT
}
