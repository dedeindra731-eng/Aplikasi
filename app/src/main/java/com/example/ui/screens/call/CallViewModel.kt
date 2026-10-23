package com.example.ui.screens.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.CallType
import com.example.data.model.User
import com.example.data.repository.CallSignalingRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallViewModel(
    private val callSignalingRepository: CallSignalingRepository
) : ViewModel() {

    val currentCall: StateFlow<CallSession?> = callSignalingRepository.currentCall

    fun startCall(caller: User, receiver: User, type: CallType) {
        viewModelScope.launch {
            callSignalingRepository.startOutgoingCall(caller, receiver, type)
        }
    }

    fun answerCall() {
        viewModelScope.launch {
            callSignalingRepository.answerIncomingCall()
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            callSignalingRepository.rejectCall()
        }
    }

    fun endCall() {
        viewModelScope.launch {
            callSignalingRepository.endCall()
        }
    }

    fun toggleMic() {
        viewModelScope.launch {
            callSignalingRepository.toggleMic()
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            callSignalingRepository.toggleVideo()
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            callSignalingRepository.toggleSpeaker()
        }
    }

    fun simulateIncomingCall(fromUser: User, type: CallType) {
        viewModelScope.launch {
            callSignalingRepository.simulateIncomingCall(fromUser, type)
        }
    }
}
