package com.example.data.repository

import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.IceCandidateModel
import com.example.data.model.SignalingMessage
import com.example.data.model.SignalingMessageType
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

interface CallSignalingRepository {
    val currentCall: StateFlow<CallSession?>
    val signalingEvents: SharedFlow<SignalingMessage>

    suspend fun startOutgoingCall(caller: User, receiver: User, callType: CallType)
    suspend fun answerIncomingCall()
    suspend fun rejectCall()
    suspend fun endCall()
    suspend fun toggleMic()
    suspend fun toggleVideo()
    suspend fun toggleSpeaker()
    suspend fun simulateIncomingCall(fromUser: User, callType: CallType)
}

class InMemoryCallSignalingRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : CallSignalingRepository {

    private val _currentCall = MutableStateFlow<CallSession?>(null)
    override val currentCall: StateFlow<CallSession?> = _currentCall.asStateFlow()

    private val _signalingEvents = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    override val signalingEvents: SharedFlow<SignalingMessage> = _signalingEvents.asSharedFlow()

    private var durationJob: Job? = null

    override suspend fun startOutgoingCall(caller: User, receiver: User, callType: CallType) {
        val newCallId = "call_" + UUID.randomUUID().toString().take(8)
        val session = CallSession(
            callId = newCallId,
            callerId = caller.uid,
            callerName = caller.displayName,
            callerAvatar = caller.avatarUrl,
            receiverId = receiver.uid,
            receiverName = receiver.displayName,
            receiverAvatar = receiver.avatarUrl,
            callType = callType,
            status = CallStatus.DIALING,
            startedAt = null,
            durationSeconds = 0
        )
        _currentCall.value = session

        // Emit WebRTC SDP Offer signaling event
        _signalingEvents.emit(
            SignalingMessage(
                callId = newCallId,
                senderId = caller.uid,
                targetId = receiver.uid,
                type = SignalingMessageType.OFFER,
                sdp = "v=0\r\no=- 20518 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE 0 1\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nm=video 9 UDP/TLS/RTP/SAVPF 96"
            )
        )

        // Simulate remote signaling progression: DIALING -> RINGING -> CONNECTING -> CONNECTED
        scope.launch {
            delay(1200)
            if (_currentCall.value?.callId == newCallId && _currentCall.value?.status == CallStatus.DIALING) {
                _currentCall.value = _currentCall.value?.copy(status = CallStatus.RINGING)
                _signalingEvents.emit(
                    SignalingMessage(
                        callId = newCallId,
                        senderId = receiver.uid,
                        targetId = caller.uid,
                        type = SignalingMessageType.RINGING
                    )
                )
            }

            delay(2500)
            if (_currentCall.value?.callId == newCallId && _currentCall.value?.status == CallStatus.RINGING) {
                _currentCall.value = _currentCall.value?.copy(status = CallStatus.CONNECTING)
                
                // Callee sends SDP Answer & ICE Candidates
                _signalingEvents.emit(
                    SignalingMessage(
                        callId = newCallId,
                        senderId = receiver.uid,
                        targetId = caller.uid,
                        type = SignalingMessageType.ANSWER,
                        sdp = "v=0\r\no=- 43210 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE 0 1\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nm=video 9 UDP/TLS/RTP/SAVPF 96"
                    )
                )

                _signalingEvents.emit(
                    SignalingMessage(
                        callId = newCallId,
                        senderId = receiver.uid,
                        targetId = caller.uid,
                        type = SignalingMessageType.ICE_CANDIDATE,
                        iceCandidate = IceCandidateModel(
                            sdpMid = "0",
                            sdpMLineIndex = 0,
                            sdp = "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host",
                            serverUrl = "stun:stun.l.google.com:19302"
                        )
                    )
                )

                delay(800)
                val startTime = System.currentTimeMillis()
                _currentCall.value = _currentCall.value?.copy(
                    status = CallStatus.CONNECTED,
                    startedAt = startTime
                )
                startTimer()
            }
        }
    }

    override suspend fun answerIncomingCall() {
        val current = _currentCall.value ?: return
        _currentCall.value = current.copy(status = CallStatus.CONNECTING)
        
        scope.launch {
            delay(600)
            _currentCall.value = current.copy(
                status = CallStatus.CONNECTED,
                startedAt = System.currentTimeMillis()
            )
            startTimer()
        }
    }

    override suspend fun rejectCall() {
        val current = _currentCall.value ?: return
        _currentCall.value = current.copy(status = CallStatus.REJECTED)
        stopTimer()
        scope.launch {
            delay(600)
            _currentCall.value = null
        }
    }

    override suspend fun endCall() {
        val current = _currentCall.value ?: return
        _currentCall.value = current.copy(status = CallStatus.ENDED)
        stopTimer()
        scope.launch {
            delay(600)
            _currentCall.value = null
        }
    }

    override suspend fun toggleMic() {
        _currentCall.value = _currentCall.value?.let {
            it.copy(isMicMuted = !it.isMicMuted)
        }
    }

    override suspend fun toggleVideo() {
        _currentCall.value = _currentCall.value?.let {
            it.copy(isVideoEnabled = !it.isVideoEnabled)
        }
    }

    override suspend fun toggleSpeaker() {
        _currentCall.value = _currentCall.value?.let {
            it.copy(isSpeakerOn = !it.isSpeakerOn)
        }
    }

    override suspend fun simulateIncomingCall(fromUser: User, callType: CallType) {
        val callId = "call_" + UUID.randomUUID().toString().take(8)
        _currentCall.value = CallSession(
            callId = callId,
            callerId = fromUser.uid,
            callerName = fromUser.displayName,
            callerAvatar = fromUser.avatarUrl,
            receiverId = "me",
            receiverName = "Me",
            receiverAvatar = "",
            callType = callType,
            status = CallStatus.RINGING
        )
    }

    private fun startTimer() {
        stopTimer()
        durationJob = scope.launch {
            while (isActive) {
                delay(1000)
                _currentCall.value = _currentCall.value?.let {
                    it.copy(durationSeconds = it.durationSeconds + 1)
                }
            }
        }
    }

    private fun stopTimer() {
        durationJob?.cancel()
        durationJob = null
    }
}
