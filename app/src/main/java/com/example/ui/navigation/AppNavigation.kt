package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChatRoom
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallSignalingRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactRepository
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.call.CallScreen
import com.example.ui.screens.call.CallViewModel
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.ChatViewModel
import com.example.ui.screens.contacts.AddFriendScanScreen
import com.example.ui.screens.contacts.ContactsViewModel
import com.example.ui.screens.home.HomeScreen

sealed interface Screen {
    data object Home : Screen
    data class Chat(val room: ChatRoom) : Screen
    data object ScanBarcode : Screen
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    callSignalingRepository: CallSignalingRepository
) {
    val authViewModel = remember { AuthViewModel(authRepository) }
    val chatViewModel = remember { ChatViewModel(chatRepository) }
    val contactsViewModel = remember { ContactsViewModel(contactRepository) }
    val callViewModel = remember { CallViewModel(callSignalingRepository) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val activeCall by callViewModel.currentCall.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentUser == null) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    currentScreen = Screen.Home
                }
            )
        } else {
            val user = currentUser!!
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        currentUser = user,
                        chatRepository = chatRepository,
                        contactsViewModel = contactsViewModel,
                        callViewModel = callViewModel,
                        authViewModel = authViewModel,
                        onNavigateToChat = { room ->
                            currentScreen = Screen.Chat(room)
                        },
                        onNavigateToScan = {
                            currentScreen = Screen.ScanBarcode
                        }
                    )
                }
                is Screen.Chat -> {
                    ChatDetailScreen(
                        room = screen.room,
                        currentUser = user,
                        chatViewModel = chatViewModel,
                        callViewModel = callViewModel,
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }
                is Screen.ScanBarcode -> {
                    AddFriendScanScreen(
                        contactsViewModel = contactsViewModel,
                        currentUser = user,
                        chatRepository = chatRepository,
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        onOpenChat = { room ->
                            currentScreen = Screen.Chat(room)
                        }
                    )
                }
            }
        }

        // Global High-priority WebRTC Call Overlay (Ringing / Connected)
        AnimatedVisibility(
            visible = activeCall != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            activeCall?.let { call ->
                CallScreen(
                    callSession = call,
                    callViewModel = callViewModel,
                    onCallClosed = {
                        // Dismiss call overlay
                    }
                )
            }
        }
    }
}
