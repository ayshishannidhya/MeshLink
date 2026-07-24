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

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevConsole: () -> Unit
) {
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Identity section
            SettingsSection("Identity") {
                SettingsItem(Icons.Outlined.Fingerprint, "Key Fingerprint", "Tap to view")
                SettingsItem(Icons.Outlined.Badge, "Display Name", "MeshLink User")
                SettingsItem(Icons.Outlined.Image, "Avatar", "Choose avatar")
            }

            // Transport section
            SettingsSection("Transport") {
                SettingsToggle(Icons.Outlined.Bluetooth, "Bluetooth LE", true)
                SettingsToggle(Icons.Outlined.Wifi, "Wi-Fi Direct", true)
                SettingsToggle(Icons.Outlined.Lan, "Local Network", false)
            }

            // Battery section
            SettingsSection("Battery") {
                SettingsItem(Icons.Outlined.BatterySaver, "Power Mode", "Balanced")
                SettingsItem(Icons.Outlined.Speed, "Scan Interval", "Normal (4s/8s)")
            }

            // Security section
            SettingsSection("Security") {
                SettingsItem(Icons.Outlined.DeleteForever, "Emergency Wipe", "Clear all data")
                SettingsItem(Icons.Outlined.Backup, "Encrypted Backup", "Export/Import")
            }

            // Developer section
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
                            Text("Packet logs, routing table, diagnostics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
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
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    defaultEnabled: Boolean
) {
    var enabled by remember { mutableStateOf(defaultEnabled) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
}
