package com.example.ui.screens.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType

@Composable
fun CallScreen(
    callSession: CallSession,
    callViewModel: CallViewModel,
    onCallClosed: () -> Unit
) {
    BackHandler {
        callViewModel.endCall()
        onCallClosed()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Audio & Camera permissions
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            )
        }
    }

    // Call duration formatted mm:ss
    val durationText = remember(callSession.durationSeconds) {
        val minutes = callSession.durationSeconds / 60
        val seconds = callSession.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // Camera Selector for PIP view (Front / Back)
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Video Remote Feed or Voice Canvas
        if (callSession.callType == CallType.VIDEO && callSession.status == CallStatus.CONNECTED) {
            // Simulated High-fidelity remote video feed canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = callSession.receiverAvatar.ifBlank {
                            "https://images.unsplash.com/photo-1494790108377-be9c29b29330"
                        },
                        contentDescription = "Remote Callee",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF0284C7), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "WebRTC Video Aktif (P2P 1080p)",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Local Camera PIP (Picture-In-Picture) View
            if (callSession.isVideoEnabled) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 20.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (e: Exception) {
                                    // fallback if front camera unavailable on emulator
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                    } catch (_: Exception) {}
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Voice Call Audio Pulse Canvas
            VoiceCallAvatarCanvas(
                avatarUrl = callSession.receiverAvatar,
                displayName = callSession.receiverName,
                status = callSession.status
            )
        }

        // Top Header: Callee Name and Call Status
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = callSession.receiverName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x66000000),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (callSession.status) {
                        CallStatus.DIALING -> "Memanggil..."
                        CallStatus.RINGING -> "Berdering..."
                        CallStatus.CONNECTING -> "Menghubungkan WebRTC P2P..."
                        CallStatus.CONNECTED -> durationText
                        CallStatus.ENDED -> "Panggilan Berakhir"
                        CallStatus.REJECTED -> "Panggilan Ditolak"
                        CallStatus.BUSY -> "Sedang Sibuk"
                        CallStatus.FAILED -> "Koneksi Gagal"
                        CallStatus.IDLE -> ""
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (callSession.status == CallStatus.CONNECTED) Color(0xFF10B981) else Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom Controls Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xDD1E293B),
            tonalElevation = 12.dp
        ) {
            if (callSession.status == CallStatus.RINGING && callSession.callerId != "me") {
                // Callee Incoming Call Answer / Decline Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Button
                    IconButton(
                        onClick = { callViewModel.rejectCall() },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("btn_reject_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Tolak Panggilan",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Answer Button
                    IconButton(
                        onClick = { callViewModel.answerCall() },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .testTag("btn_answer_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Terima Panggilan",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                // Outgoing / Active Call Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Audio Toggle
                    IconButton(
                        onClick = { callViewModel.toggleMic() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (callSession.isMicMuted) Color(0xFFEF4444) else Color(0x33FFFFFF))
                            .testTag("btn_toggle_mic")
                    ) {
                        Icon(
                            imageVector = if (callSession.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic",
                            tint = Color.White
                        )
                    }

                    // Video Camera Toggle (only if video call)
                    if (callSession.callType == CallType.VIDEO) {
                        IconButton(
                            onClick = { callViewModel.toggleVideo() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (!callSession.isVideoEnabled) Color(0xFFEF4444) else Color(0x33FFFFFF))
                                .testTag("btn_toggle_video")
                        ) {
                            Icon(
                                imageVector = if (callSession.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Toggle Video",
                                tint = Color.White
                            )
                        }

                        // Camera Flip Switch
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                    CameraSelector.LENS_FACING_BACK
                                else CameraSelector.LENS_FACING_FRONT
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .testTag("btn_switch_camera")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }

                    // Speakerphone Toggle
                    IconButton(
                        onClick = { callViewModel.toggleSpeaker() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (callSession.isSpeakerOn) Color(0xFF0284C7) else Color(0x33FFFFFF))
                            .testTag("btn_toggle_speaker")
                    ) {
                        Icon(
                            imageVector = if (callSession.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Toggle Speaker",
                            tint = Color.White
                        )
                    }

                    // End Call Button (Red Hangup)
                    IconButton(
                        onClick = {
                            callViewModel.endCall()
                            onCallClosed()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("btn_end_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Akhiri Panggilan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceCallAvatarCanvas(
    avatarUrl: String,
    displayName: String,
    status: CallStatus
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_audio")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Ripple glow rings
            if (status == CallStatus.CONNECTED || status == CallStatus.RINGING) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0x220284C7))
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale * 0.95f)
                        .clip(CircleShape)
                        .background(Color(0x330284C7))
                )
            }

            AsyncImage(
                model = avatarUrl.ifBlank {
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330"
                },
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF38BDF8), CircleShape)
            )
        }
    }
}
