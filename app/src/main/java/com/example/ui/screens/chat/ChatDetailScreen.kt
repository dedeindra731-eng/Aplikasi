package com.example.ui.screens.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.User
import com.example.ui.screens.call.CallViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    room: ChatRoom,
    currentUser: User,
    chatViewModel: ChatViewModel,
    callViewModel: CallViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(room) {
        chatViewModel.setRoom(room.id, room.otherUser)
    }

    val uiState by chatViewModel.uiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val isTyping by chatViewModel.isOtherUserTyping.collectAsState()
    val listState = rememberLazyListState()

    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Voice Note Recording State
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var recordingDurationSecs by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingVoiceNote) {
        if (isRecordingVoiceNote) {
            recordingDurationSecs = 0
            while (isActive && isRecordingVoiceNote) {
                delay(1000)
                recordingDurationSecs += 1
            }
        }
    }

    // 1. Photo Picker Launcher (Storage Image Access)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            chatViewModel.sendImage(currentUser.uid, uri.toString())
            Toast.makeText(context, "Gambar terkirim", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Video Picker Launcher (Storage Video Access)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            chatViewModel.sendVideo(currentUser.uid, uri.toString())
            Toast.makeText(context, "Video terkirim", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Document / File Picker Launcher (Storage Document Access)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileNameAndSize(context, uri)
            chatViewModel.sendFile(currentUser.uid, uri.toString(), name, size)
            Toast.makeText(context, "Berkas $name terkirim", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. Voice Note Audio Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecordingVoiceNote = true
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk merekam voice note", Toast.LENGTH_SHORT).show()
        }
    }

    // 5. GPS Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchAndShareLocation(context) { lat, lng, name ->
                chatViewModel.sendLocation(currentUser.uid, lat, lng, name)
                Toast.makeText(context, "Lokasi berhasil dibagikan", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk berbagi lokasi", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
                                model = room.otherUser.avatarUrl.ifBlank {
                                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330"
                                },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            if (room.otherUser.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = room.otherUser.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTyping) "sedang mengetik..." else if (room.otherUser.isOnline) "Online" else "Offline",
                                fontSize = 12.sp,
                                color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            callViewModel.startCall(currentUser, room.otherUser, CallType.AUDIO)
                        },
                        modifier = Modifier.testTag("chat_action_audio_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Panggilan Suara",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            callViewModel.startCall(currentUser, room.otherUser, CallType.VIDEO)
                        },
                        modifier = Modifier.testTag("chat_action_video_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Panggilan Video",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMe = message.senderId == currentUser.uid
                    RichMessageBubble(message = message, isMe = isMe, context = context)
                }

                if (isTyping) {
                    item {
                        TypingIndicatorBubble(otherUserName = room.otherUser.displayName)
                    }
                }
            }

            // Bottom Input & Voice Recording Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (isRecordingVoiceNote) {
                        // Voice Note Recording Active Bar
                        VoiceRecordingBar(
                            durationSecs = recordingDurationSecs,
                            onCancel = {
                                isRecordingVoiceNote = false
                            },
                            onSend = {
                                isRecordingVoiceNote = false
                                val duration = maxOf(1, recordingDurationSecs)
                                chatViewModel.sendVoiceNote(
                                    currentUser.uid,
                                    "vn_${System.currentTimeMillis()}.m4a",
                                    duration
                                )
                                Toast.makeText(context, "Voice note terkirim (${duration}s)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        // Standard Input Row with Attachment & Mic
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attachment Menu Button
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                modifier = Modifier.testTag("btn_attachment_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Lampiran",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Text Input Field
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { chatViewModel.onInputTextChanged(it) },
                                placeholder = { Text("Tulis pesan...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_text_field")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Send or Voice Note Action Button
                            if (uiState.inputText.isNotBlank()) {
                                IconButton(
                                    onClick = { chatViewModel.sendMessage(currentUser.uid) },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Kirim",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                // Microphone / Voice Note Button
                                IconButton(
                                    onClick = {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            isRecordingVoiceNote = true
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("btn_voice_note_record")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Rekam Voice Note",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Kirim Lampiran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // 1. Gambar / Foto
                    AttachmentOptionItem(
                        icon = Icons.Default.Image,
                        label = "Foto / Gambar",
                        backgroundColor = Color(0xFF0284C7),
                        onClick = {
                            showAttachmentMenu = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    // 2. Video
                    AttachmentOptionItem(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        backgroundColor = Color(0xFF8B5CF6),
                        onClick = {
                            showAttachmentMenu = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )

                    // 3. Dokumen / File
                    AttachmentOptionItem(
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        label = "Dokumen / File",
                        backgroundColor = Color(0xFF10B981),
                        onClick = {
                            showAttachmentMenu = false
                            filePickerLauncher.launch("*/*")
                        }
                    )

                    // 4. Share Lokasi
                    AttachmentOptionItem(
                        icon = Icons.Default.LocationOn,
                        label = "Share Lokasi",
                        backgroundColor = Color(0xFFEF4444),
                        onClick = {
                            showAttachmentMenu = false
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasFine || hasCoarse) {
                                fetchAndShareLocation(context) { lat, lng, name ->
                                    chatViewModel.sendLocation(currentUser.uid, lat, lng, name)
                                    Toast.makeText(context, "Lokasi dibagikan: $name", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VoiceRecordingBar(
    durationSecs: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rec")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_dot"
    )

    val minutes = durationSecs / 60
    val seconds = durationSecs % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blinking Red Recording Dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Merekam: $timeFormatted",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF4444)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Cancel Button
        IconButton(
            onClick = onCancel,
            modifier = Modifier.testTag("btn_cancel_voice_record")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Batal",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send Voice Note Button
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("btn_send_voice_note")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Kirim Voice Note",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun RichMessageBubble(
    message: Message,
    isMe: Boolean,
    context: Context
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (message.type) {
                // 1. Image message bubble
                MessageType.IMAGE -> {
                    AsyncImage(
                        model = message.mediaUri ?: "https://images.unsplash.com/photo-1579202673506-ca3ce28943ef",
                        contentDescription = "Foto terkirim",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. Video message bubble
                MessageType.VIDEO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Putar Video",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x99000000),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Video • 00:24",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. Generic File / Document bubble
                MessageType.FILE -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMe) Color(0x33FFFFFF) else Color(0x22000000),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "Dokumen.pdf",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = message.fileSizeFormatted ?: "1.4 MB",
                                    fontSize = 11.sp,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // 4. Voice Note bubble
                MessageType.VOICE_NOTE -> {
                    var isPlaying by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color(0x33FFFFFF) else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Audio waveform bars visualization
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(18.dp)
                            ) {
                                val heights = listOf(6, 12, 18, 10, 14, 8, 16, 12, 18, 14, 10, 8, 14, 6)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isMe) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "00:${String.format("%02d", message.durationSeconds)}",
                                fontSize = 11.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 5. Location message bubble
                MessageType.LOCATION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "Peta Lokasi GPS",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = message.locationName ?: "Lokasi Terkini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Lat: ${message.latitude ?: -6.2088}, Lng: ${message.longitude ?: 106.8456}",
                            fontSize = 11.sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                val lat = message.latitude ?: -6.2088
                                val lng = message.longitude ?: 106.8456
                                val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(mapIntent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMe) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka di Google Maps", fontSize = 12.sp)
                        }
                    }
                }

                // 6. Default Text message
                else -> {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Message timestamp and delivery status tick
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (isMe) {
                    when (message.status) {
                        MessageStatus.SENDING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        MessageStatus.SENT -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terkirim",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Diterima",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        MessageStatus.READ -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Dibaca",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun queryFileNameAndSize(context: Context, uri: Uri): Pair<String, String> {
    var name = "Dokumen_${System.currentTimeMillis()}"
    var sizeFormatted = "1.2 MB"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) {
                    val bytes = cursor.getLong(sizeIndex)
                    sizeFormatted = if (bytes > 1024 * 1024) {
                        String.format("%.1f MB", bytes / (1024f * 1024f))
                    } else {
                        String.format("%d KB", bytes / 1024)
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return Pair(name, sizeFormatted)
}

@SuppressLint("MissingPermission")
private fun fetchAndShareLocation(
    context: Context,
    onLocationReady: (Double, Double, String) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onLocationReady(loc.latitude, loc.longitude, "Koordinat (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})")
            } else {
                // Fallback coordinates (e.g. Jakarta Pusat coordinates for emulator without mock provider)
                onLocationReady(-6.2088, 106.8456, "Jakarta Pusat, Indonesia")
            }
        }.addOnFailureListener {
            onLocationReady(-6.2088, 106.8456, "Jakarta Pusat, Indonesia")
        }
    } catch (e: Exception) {
        onLocationReady(-6.2088, 106.8456, "Jakarta Pusat, Indonesia")
    }
}

@Composable
fun TypingIndicatorBubble(otherUserName: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$otherUserName sedang mengetik...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
