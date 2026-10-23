package com.example.ui.screens.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ChatRoom
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.ui.components.CameraBarcodeScannerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScanScreen(
    contactsViewModel: ContactsViewModel,
    currentUser: User,
    chatRepository: ChatRepository,
    onNavigateBack: () -> Unit,
    onOpenChat: (ChatRoom) -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcodeText by remember { mutableStateOf("") }
    var scannedUserResult by remember { mutableStateOf<User?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Barcode / QR Teman") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_scanner")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                // Live CameraX + Google ML Kit Barcode Analyzer
                CameraBarcodeScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeScanned = { rawBarcode ->
                        contactsViewModel.addFriendFromBarcode(rawBarcode) { success, user ->
                            if (success && user != null) {
                                scannedUserResult = user
                                showSuccessDialog = true
                            }
                        }
                    }
                )
            } else {
                // Camera permission fallback view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera Diperlukan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aktifkan izin kamera untuk memindai Barcode ID teman secara otomatis.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("btn_grant_camera_permission")
                    ) {
                        Text("Beri Izin Kamera")
                    }
                }
            }

            // Bottom card for Manual Barcode ID input or quick testing
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Atau Masukkan Barcode ID Manual:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeText,
                            onValueChange = { manualBarcodeText = it },
                            placeholder = { Text("Contoh: LJ-UID-usr_anita_004") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualBarcodeText.isNotBlank()) {
                                    contactsViewModel.addFriendFromBarcode(manualBarcodeText) { success, user ->
                                        if (success && user != null) {
                                            scannedUserResult = user
                                            showSuccessDialog = true
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_add_manual_barcode")
                        ) {
                            Text("Tambah")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Uji Coba Cepat (Simulasi scan barcode):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                manualBarcodeText = "LJ-UID-usr_anita_004"
                                contactsViewModel.addFriendFromBarcode(manualBarcodeText) { _, user ->
                                    scannedUserResult = user
                                    showSuccessDialog = true
                                }
                            },
                            label = { Text("Anita (DevOps)", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                manualBarcodeText = "LJ-UID-usr_fadli_005"
                                contactsViewModel.addFriendFromBarcode(manualBarcodeText) { _, user ->
                                    scannedUserResult = user
                                    showSuccessDialog = true
                                }
                            },
                            label = { Text("Fadli (WebRTC)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Success Dialog when Barcode is identified
        if (showSuccessDialog && scannedUserResult != null) {
            val user = scannedUserResult!!
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = "Teman Ditemukan!",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = user.avatarUrl.ifBlank {
                                "https://images.unsplash.com/photo-1517841905240-472988babdf9"
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = user.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = user.barcodeId,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = user.bio,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            scope.launch {
                                val room = chatRepository.getOrCreateRoom(currentUser.uid, user)
                                onOpenChat(room)
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_chat_scanned")
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim Pesan Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}
