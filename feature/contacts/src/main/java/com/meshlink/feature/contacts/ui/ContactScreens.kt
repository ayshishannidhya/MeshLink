/*
 * =============================================================================
 * MeshLink
 * Secure Offline Mesh Communication Platform
 *
 * Copyright (c) 2026 Ayshi Shannidhya Panda.
 * All Rights Reserved.
 *
 * MeshLink, the MeshLink Protocol, associated software, source code,
 * documentation, algorithms, and design architecture are proprietary
 * intellectual property of Ayshi Shannidhya Panda.
 *
 * Unauthorized reproduction, modification, distribution, or commercial
 * exploitation of any part of this software or protocol is prohibited
 * without prior written permission.
 *
 * Author  : Ayshi Shannidhya Panda
 * =============================================================================
 */
package com.meshlink.feature.contacts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import java.util.concurrent.Executors

// ── Contact List Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToQrPairing: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identities", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = onNavigateToQrPairing) {
                    Icon(Icons.Outlined.QrCode2, "QR Pairing")
                }
                FloatingActionButton(onClick = onNavigateToDiscovery) {
                    Icon(Icons.Default.Search, "Discover")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.People, null, Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                Spacer(Modifier.height(16.dp))
                Text("No identities saved", style = MaterialTheme.typography.titleMedium)
                Text("Discover nearby peers or scan a QR code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Discovery Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover Peers") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(24.dp))
                Text("Scanning for nearby devices...",
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Using Bluetooth LE + Wi-Fi Direct + LAN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── QR Pairing Screen with Camera ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPairingScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showScanner = true
        } else {
            Toast.makeText(context, "Camera permission needed for QR scanning", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (showScanner) "Scan QR Code" else "QR Identity Exchange")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showScanner) showScanner = false else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showScanner) {
                // ── Camera QR Scanner ────────────────────────
                QrScannerView(
                    onQrCodeScanned = { data ->
                        scannedData = data
                        showScanner = false
                        Timber.d("QR scanned: $data")
                    }
                )
            } else {
                // ── QR Display + Scan Button ─────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Show scanned result if available
                    if (scannedData != null) {
                        ScannedResultCard(
                            data = scannedData!!,
                            onDismiss = { scannedData = null }
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // QR Code display (your identity)
                    Surface(
                        modifier = Modifier.size(240.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Placeholder QR - in production, generate real QR from public key
                            Icon(
                                Icons.Outlined.QrCode2, null,
                                Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Your Identity QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Share this with peers for secure pairing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(32.dp))

                    // Scan button with camera permission handling
                    FilledTonalButton(
                        onClick = {
                            if (hasCameraPermission) {
                                showScanner = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Scan Peer's QR Code", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

// ── Camera QR Scanner Component ──────────────────────────────────────────

@Composable
private fun QrScannerView(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isScanned by remember { mutableStateOf(false) }

    // Animated scan line
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineOffset"
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    // Image analysis for QR detection
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isScanned) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                if (barcode.valueType == Barcode.TYPE_TEXT ||
                                                    barcode.valueType == Barcode.TYPE_UNKNOWN
                                                ) {
                                                    barcode.rawValue?.let { value ->
                                                        if (!isScanned) {
                                                            isScanned = true
                                                            Timber.d("QR detected: $value")
                                                            onQrCodeScanned(value)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Timber.e(e, "Barcode scanning failed")
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    // Bind to lifecycle
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        Timber.d("Camera bound successfully")
                    } catch (e: Exception) {
                        Timber.e(e, "Camera binding failed")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with scan window cutout
        ScannerOverlay(scanLineOffset = scanLineOffset)

        // Bottom instruction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.7f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "Point camera at a MeshLink QR code",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Scanner Overlay with Animated Scan Line ──────────────────────────────

@Composable
private fun ScannerOverlay(scanLineOffset: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Scanner window dimensions
        val windowSize = canvasWidth * 0.7f
        val left = (canvasWidth - windowSize) / 2f
        val top = (canvasHeight - windowSize) / 2.5f
        val right = left + windowSize
        val bottom = top + windowSize

        // Dim background around scanner window
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )

        // Cut out the scanner window (transparent)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(windowSize, windowSize),
            cornerRadius = CornerRadius(24f, 24f),
            blendMode = BlendMode.Clear
        )

        // Scanner window border
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(windowSize, windowSize),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3f)
        )

        // Corner accents (thicker lines at corners)
        val cornerLength = 40f
        val cornerStroke = 6f

        // Top-left corner
        drawLine(primaryColor, Offset(left, top + 12), Offset(left, top + cornerLength), cornerStroke)
        drawLine(primaryColor, Offset(left + 12, top), Offset(left + cornerLength, top), cornerStroke)

        // Top-right corner
        drawLine(primaryColor, Offset(right, top + 12), Offset(right, top + cornerLength), cornerStroke)
        drawLine(primaryColor, Offset(right - 12, top), Offset(right - cornerLength, top), cornerStroke)

        // Bottom-left corner
        drawLine(primaryColor, Offset(left, bottom - 12), Offset(left, bottom - cornerLength), cornerStroke)
        drawLine(primaryColor, Offset(left + 12, bottom), Offset(left + cornerLength, bottom), cornerStroke)

        // Bottom-right corner
        drawLine(primaryColor, Offset(right, bottom - 12), Offset(right, bottom - cornerLength), cornerStroke)
        drawLine(primaryColor, Offset(right - 12, bottom), Offset(right - cornerLength, bottom), cornerStroke)

        // Animated scan line
        val lineY = top + (windowSize * scanLineOffset)
        drawLine(
            color = primaryColor.copy(alpha = 0.8f),
            start = Offset(left + 16f, lineY),
            end = Offset(right - 16f, lineY),
            strokeWidth = 2f
        )
    }
}

// ── Scanned Result Card ──────────────────────────────────────────────────

@Composable
private fun ScannedResultCard(
    data: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Peer Identity Scanned",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Show truncated public key
            val displayKey = if (data.length > 64) {
                data.take(32) + "..." + data.takeLast(16)
            } else {
                data
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
            ) {
                Text(
                    text = displayKey,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    // In production: initiate Noise XX handshake with scanned public key
                    Timber.d("Initiating pairing with: $data")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Handshake, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Secure Pairing")
            }
        }
    }
}
