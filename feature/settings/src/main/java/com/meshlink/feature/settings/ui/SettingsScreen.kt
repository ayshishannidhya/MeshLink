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
package com.meshlink.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.core.common.PowerMode
import com.meshlink.core.common.ScanInterval
import com.meshlink.core.domain.model.SettingsState
import com.meshlink.core.network.transport.TransportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevConsole: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SettingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SettingsUiState.Ready -> {
                    SettingsContent(
                        settings = state.settings,
                        viewModel = viewModel,
                        onNavigateToDevConsole = onNavigateToDevConsole
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: SettingsState,
    viewModel: SettingsViewModel,
    onNavigateToDevConsole: () -> Unit
) {
    var showFingerprintDialog by remember { mutableStateOf(false) }
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    var showPowerModeDialog by remember { mutableStateOf(false) }
    var showScanIntervalDialog by remember { mutableStateOf(false) }
    var showEmergencyWipeDialog by remember { mutableStateOf(false) }
    var showEncryptedBackupDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.updateAvatarUri(it.toString()) } }
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SettingsSection("Identity") {
                SettingsClickableItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Key Fingerprint",
                    subtitle = settings.fingerprint.take(8),
                    onClick = { showFingerprintDialog = true }
                )
                SettingsClickableItem(
                    icon = Icons.Outlined.Badge,
                    title = "Display Name",
                    subtitle = settings.displayName,
                    onClick = { showDisplayNameDialog = true }
                )
                SettingsClickableItem(
                    icon = Icons.Outlined.Image,
                    title = "Avatar",
                    subtitle = if (settings.avatarUri != null) "Avatar set" else "Tap to change",
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        item {
            SettingsSection("Transport") {
                SettingsToggle(
                    icon = Icons.Outlined.Bluetooth,
                    title = "Bluetooth LE",
                    checked = settings.bleEnabled,
                    onCheckedChange = { viewModel.toggleTransport(TransportType.BLE, it) }
                )
                SettingsToggle(
                    icon = Icons.Outlined.Wifi,
                    title = "Wi-Fi Direct",
                    checked = settings.wifiDirectEnabled,
                    onCheckedChange = { viewModel.toggleTransport(TransportType.WIFI_DIRECT, it) }
                )
                SettingsToggle(
                    icon = Icons.Outlined.Lan,
                    title = "Local Network",
                    checked = settings.lanEnabled,
                    onCheckedChange = { viewModel.toggleTransport(TransportType.LAN, it) }
                )
            }
        }

        item {
            SettingsSection("Battery") {
                SettingsClickableItem(
                    icon = Icons.Outlined.BatterySaver,
                    title = "Power Mode",
                    subtitle = settings.powerMode.name.replace("_", " "),
                    onClick = { showPowerModeDialog = true }
                )
                SettingsClickableItem(
                    icon = Icons.Outlined.Speed,
                    title = "Scan Interval",
                    subtitle = settings.scanInterval.name,
                    onClick = { showScanIntervalDialog = true }
                )
            }
        }

        item {
            SettingsSection("Security") {
                SettingsClickableItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Emergency Wipe",
                    subtitle = "Clear all data",
                    onClick = { showEmergencyWipeDialog = true },
                    isDestructive = true
                )
                SettingsClickableItem(
                    icon = Icons.Outlined.Backup,
                    title = "Encrypted Backup",
                    subtitle = "Export / Import",
                    onClick = { showEncryptedBackupDialog = true }
                )
            }
        }

        item {
            SettingsSection("Developer") {
                Surface(
                    onClick = onNavigateToDevConsole,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Developer Console", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Packet logs, routing table, diagnostics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFingerprintDialog) {
        FingerprintDialog(
            fingerprint = settings.fingerprint,
            onDismiss = { showFingerprintDialog = false }
        )
    }

    if (showDisplayNameDialog) {
        EditDisplayNameDialog(
            currentName = settings.displayName,
            onConfirm = { 
                viewModel.updateDisplayName(it)
                showDisplayNameDialog = false
            },
            onDismiss = { showDisplayNameDialog = false }
        )
    }

    if (showPowerModeDialog) {
        PowerModeDialog(
            currentMode = settings.powerMode,
            onConfirm = { 
                viewModel.setPowerMode(it)
                showPowerModeDialog = false
            },
            onDismiss = { showPowerModeDialog = false }
        )
    }

    if (showScanIntervalDialog) {
        ScanIntervalDialog(
            currentInterval = settings.scanInterval,
            onConfirm = { 
                viewModel.setScanInterval(it)
                showScanIntervalDialog = false
            },
            onDismiss = { showScanIntervalDialog = false }
        )
    }

    if (showEmergencyWipeDialog) {
        EmergencyWipeDialog(
            onConfirm = {
                viewModel.performEmergencyWipe()
                showEmergencyWipeDialog = false
            },
            onDismiss = { showEmergencyWipeDialog = false }
        )
    }

    if (showEncryptedBackupDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptedBackupDialog = false },
            title = { Text("Encrypted Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Your identity keys and contacts are stored locally on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "⚠️ If you lose this device, your identity cannot be recovered without a backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Backup includes:\n• Identity keypair\n• Saved contacts\n• Conversation history\n• Trust scores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showEncryptedBackupDialog = false }) {
                    Text("Export Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEncryptedBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FingerprintDialog(fingerprint: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val formattedFingerprint = fingerprint.chunked(4).joinToString(" ")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Key Fingerprint") },
        text = { Text(formattedFingerprint) },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fingerprint))
                    onDismiss()
                }
            ) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EditDisplayNameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PowerModeDialog(currentMode: PowerMode, onConfirm: (PowerMode) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(currentMode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Power Mode") },
        text = {
            Column {
                PowerMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selected = mode }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selected == mode, onClick = { selected = mode })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.name.replace("_", " "))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScanIntervalDialog(currentInterval: ScanInterval, onConfirm: (ScanInterval) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(currentInterval) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Interval") },
        text = {
            Column {
                ScanInterval.entries.forEach { interval ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selected = interval }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selected == interval, onClick = { selected = interval })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(interval.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmergencyWipeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Emergency Wipe") },
        text = {
            Column {
                Text("This will permanently delete ALL data.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Type 'WIPE' to confirm") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = text == "WIPE",
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
