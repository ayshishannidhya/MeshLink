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
package com.meshlink.feature.devtools.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Developer console for mesh diagnostics.
 *
 * Tabs:
 * - Packets: Live packet log stream
 * - Routes: Current routing table
 * - Stats: Transport-level statistics
 * - Peers: Neighbor table dump
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevConsoleScreen(onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Packets", "Routes", "Stats", "Peers")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PacketLogTab()
                1 -> RoutingTableTab()
                2 -> StatsTab()
                3 -> PeersTab()
            }
        }
    }
}

@Composable
private fun PacketLogTab() {
    val demoLogs = listOf(
        "09:54:01.234 TX ANNOUNCE ttl=7 id=a1b2c3d4",
        "09:54:02.567 RX MESSAGE  ttl=5 id=e5f67890 from=deadbeef",
        "09:54:03.891 TX ACK      ttl=7 id=12345678 to=deadbeef",
        "09:54:05.123 RX ANNOUNCE ttl=6 id=87654321 from=cafebabe",
        "09:54:06.456 RELAY MSG   ttl=4 id=e5f67890 â†’ cafebabe",
        "09:54:08.789 RX NOISE_HS ttl=7 id=abcdef01 from=deadbeef",
        "09:54:09.012 TX NOISE_HS ttl=7 id=fedcba98 to=deadbeef",
        "09:54:10.345 SESSION     deadbeef â†’ ESTABLISHED (Noise XX)",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(demoLogs) { log ->
            Text(
                text = log,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = when {
                    log.contains("TX") -> MaterialTheme.colorScheme.primary
                    log.contains("RX") -> MaterialTheme.colorScheme.tertiary
                    log.contains("RELAY") -> MaterialTheme.colorScheme.secondary
                    log.contains("SESSION") -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun RoutingTableTab() {
    val routes = listOf(
        Triple("deadbeef", "direct (BLE)", "1 hop"),
        Triple("cafebabe", "via deadbeef", "2 hops"),
        Triple("12345678", "direct (WiFi)", "1 hop"),
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Destination", Modifier.weight(1f), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
                Text("Next Hop", Modifier.weight(1f), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
                Text("Distance", Modifier.weight(0.5f), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
        }
        items(routes) { (dest, nextHop, hops) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(dest, Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp)
                Text(nextHop, Modifier.weight(1f), fontSize = 13.sp)
                Text(hops, Modifier.weight(0.5f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatsTab() {
    val stats = listOf(
        "Packets Sent" to "342",
        "Packets Received" to "518",
        "Packets Relayed" to "176",
        "Packets Dropped (dedup)" to "89",
        "Packets Dropped (rate limit)" to "3",
        "Active Sessions (Noise)" to "2",
        "SAF Queue Size" to "5",
        "BLE MTU" to "512 bytes",
        "Avg Latency" to "45ms",
        "Uptime" to "2h 34m",
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(stats) { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun PeersTab() {
    val peers = listOf(
        PeerInfo("deadbeef", "Alice", -55, "BLE", true, 0.85f),
        PeerInfo("cafebabe", "Bob", -72, "BLE", true, 0.62f),
        PeerInfo("12345678", "Charlie", -40, "WiFi", true, 0.91f),
        PeerInfo("87654321", "Unknown", -88, "BLE", false, 0.30f),
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(peers) { peer ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(peer.name, fontWeight = FontWeight.Bold)
                        Text(if (peer.active) "â— Active" else "â—‹ Stale",
                            color = if (peer.active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("ID: ${peer.id}", fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("RSSI: ${peer.rssi}dBm", fontSize = 12.sp)
                        Text("Transport: ${peer.transport}", fontSize = 12.sp)
                        Text("Trust: ${"%.0f".format(peer.trust * 100)}%", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private data class PeerInfo(
    val id: String, val name: String, val rssi: Int,
    val transport: String, val active: Boolean, val trust: Float
)
