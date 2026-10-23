package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactRepository
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.call.CallViewModel
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.contacts.ContactsViewModel
import com.example.ui.screens.profile.MyQrProfileScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HomeTab(val label: String) {
    CHATS("Obrolan"),
    CONTACTS("Kontak"),
    CALLS("Panggilan"),
    MY_QR("QR Profil")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User,
    chatRepository: ChatRepository,
    contactsViewModel: ContactsViewModel,
    callViewModel: CallViewModel,
    authViewModel: AuthViewModel,
    onNavigateToChat: (ChatRoom) -> Unit,
    onNavigateToScan: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.CHATS) }
    val chatRooms by chatRepository.chatRooms.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = currentUser.avatarUrl.ifBlank {
                                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
                                },
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Column {
                            Text(
                                text = "Leuwijaya Chat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser.displayName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToScan,
                        modifier = Modifier.testTag("top_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Pindai Barcode ID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.CHATS,
                    onClick = { selectedTab = HomeTab.CHATS },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = null) },
                    label = { Text("Obrolan") },
                    modifier = Modifier.testTag("nav_tab_chats")
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.CONTACTS,
                    onClick = { selectedTab = HomeTab.CONTACTS },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Kontak") },
                    modifier = Modifier.testTag("nav_tab_contacts")
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.CALLS,
                    onClick = { selectedTab = HomeTab.CALLS },
                    icon = { Icon(Icons.Default.Call, contentDescription = null) },
                    label = { Text("Panggilan") },
                    modifier = Modifier.testTag("nav_tab_calls")
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.MY_QR,
                    onClick = { selectedTab = HomeTab.MY_QR },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    label = { Text("QR Profil") },
                    modifier = Modifier.testTag("nav_tab_my_qr")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.CHATS || selectedTab == HomeTab.CONTACTS) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scan Barcode") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_scan_barcode")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                HomeTab.CHATS -> {
                    ChatsListContent(
                        chatRooms = chatRooms,
                        onRoomClick = onNavigateToChat
                    )
                }
                HomeTab.CONTACTS -> {
                    ContactsScreen(
                        contactsViewModel = contactsViewModel,
                        currentUser = currentUser,
                        chatRepository = chatRepository,
                        callViewModel = callViewModel,
                        onOpenChat = onNavigateToChat,
                        onOpenScanner = onNavigateToScan
                    )
                }
                HomeTab.CALLS -> {
                    CallsHistoryContent(
                        currentUser = currentUser,
                        callViewModel = callViewModel,
                        contactsViewModel = contactsViewModel
                    )
                }
                HomeTab.MY_QR -> {
                    MyQrProfileScreen(
                        currentUser = currentUser,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ChatsListContent(
    chatRooms: List<ChatRoom>,
    onRoomClick: (ChatRoom) -> Unit
) {
    if (chatRooms.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Belum ada obrolan aktif",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pindai Barcode ID teman untuk memulai chat!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatRooms) { room ->
                ChatRoomItem(room = room, onClick = { onRoomClick(room) })
            }
        }
    }
}

@Composable
fun ChatRoomItem(
    room: ChatRoom,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(room.lastMessageTimestamp) {
        timeFormat.format(Date(room.lastMessageTimestamp))
    }

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_room_item_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = room.otherUser.avatarUrl.ifBlank {
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330"
                    },
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
                if (room.otherUser.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.otherUser.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (room.isOtherUserTyping) "sedang mengetik..." else room.lastMessage,
                        fontSize = 13.sp,
                        color = if (room.isOtherUserTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (room.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    if (room.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(text = room.unreadCount.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsHistoryContent(
    currentUser: User,
    callViewModel: CallViewModel,
    contactsViewModel: ContactsViewModel
) {
    val contacts by contactsViewModel.filteredContacts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Panggilan Cepat (WebRTC P2P)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Mulai panggilan suara atau video berkualitas tinggi",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(contacts) { contact ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = contact.avatarUrl.ifBlank {
                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d"
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (contact.isOnline) "Tersedia untuk panggilan" else "Offline",
                                fontSize = 12.sp,
                                color = if (contact.isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Voice Call Button
                        IconButton(
                            onClick = {
                                callViewModel.startCall(currentUser, contact, CallType.AUDIO)
                            },
                            modifier = Modifier.testTag("quick_audio_call_${contact.uid}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Panggilan Suara",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Video Call Button
                        IconButton(
                            onClick = {
                                callViewModel.startCall(currentUser, contact, CallType.VIDEO)
                            },
                            modifier = Modifier.testTag("quick_video_call_${contact.uid}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Panggilan Video",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Simulator Button for incoming call test
                OutlinedButton(
                    onClick = {
                        val firstContact = contacts.firstOrNull() ?: User(
                            uid = "usr_siti_002",
                            displayName = "Siti Nurhaliza"
                        )
                        callViewModel.simulateIncomingCall(firstContact, CallType.VIDEO)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_simulate_incoming_call")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uji Coba: Terima Panggilan Masuk (Simulasi)")
                }
            }
        }
    }
}
