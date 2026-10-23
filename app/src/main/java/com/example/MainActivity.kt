package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.InMemoryAuthRepository
import com.example.data.repository.InMemoryCallSignalingRepository
import com.example.data.repository.InMemoryChatRepository
import com.example.data.repository.InMemoryContactRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.LeuwijayaChatTheme

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { InMemoryAuthRepository() }
    private val contactRepository by lazy { InMemoryContactRepository() }
    private val chatRepository by lazy { InMemoryChatRepository() }
    private val callSignalingRepository by lazy { InMemoryCallSignalingRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeuwijayaChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        authRepository = authRepository,
                        chatRepository = chatRepository,
                        contactRepository = contactRepository,
                        callSignalingRepository = callSignalingRepository
                    )
                }
            }
        }
    }
}
