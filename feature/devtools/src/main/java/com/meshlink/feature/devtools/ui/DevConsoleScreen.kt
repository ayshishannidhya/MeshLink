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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshlink.core.common.toShortHex

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
fun DevConsoleScreen(
    onNavigateBack: () -> Unit,
    viewModel: DevConsoleViewModel = hiltViewModel()
) {
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
                0 -> PacketLogTab(viewModel)
                1 -> RoutingTableTab(viewModel)
                2 -> StatsTab(viewModel)
                3 -> PeersTab(viewModel)
            }
        }
    }
}

@Composable
private fun PacketLogTab(viewModel: DevConsoleViewModel) {
    val logs by viewModel.packetLog.collectAsState()

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No packets captured yet. Waiting for mesh traffic...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { log ->
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
}

@Composable
private fun RoutingTableTab(viewModel: DevConsoleViewModel) {
    val activePeers by viewModel.activePeers.collectAsState()

    if (activePeers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No routes. Discover peers to build routing table.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Destination", Modifier.weight(1f), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                    Text("Next Hop", Modifier.weight(1f), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                    Text("Transport", Modifier.weight(1f), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                    Text("Hops", Modifier.weight(0.5f), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                }
                HorizontalDivider()
            }
            items(activePeers) { peer ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(peer.peerId.toShortHex(), Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp)
                    Text("direct", Modifier.weight(1f), fontSize = 13.sp)
                    Text(peer.bestTransport.displayName, Modifier.weight(1f), fontSize = 13.sp)
                    Text("1", Modifier.weight(0.5f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatsTab(viewModel: DevConsoleViewModel) {
    val statsState by viewModel.meshStats.collectAsState()

    val uptimeSecs = statsState.uptimeMs / 1000
    val h = uptimeSecs / 3600
    val m = (uptimeSecs % 3600) / 60
    val s = uptimeSecs % 60
    val uptimeStr = "${h}h ${m}m ${s}s"

    val stats = listOf(
        "Packets Sent" to statsState.packetsSent.toString(),
        "Packets Received" to statsState.packetsReceived.toString(),
        "Packets Relayed" to statsState.packetsRelayed.toString(),
        "Packets Dropped" to statsState.packetsDropped.toString(),
        "Active Peers" to statsState.activePeers.toString(),
        "Pending Messages (SAF)" to statsState.pendingMessages.toString(),
        "Avg Hop Count" to "%.1f".format(statsState.averageHopCount),
        "Uptime" to uptimeStr
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
private fun PeersTab(viewModel: DevConsoleViewModel) {
    val activePeers by viewModel.activePeers.collectAsState()

    if (activePeers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No peers detected. Make sure Bluetooth is enabled.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(activePeers) { peer ->
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
                            Text(peer.displayName ?: "Unknown", fontWeight = FontWeight.Bold)
                            Text(if (peer.isActive) "● Active" else "○ Stale",
                                color = if (peer.isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Text("ID: ${peer.peerId.toShortHex()}", fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("RSSI: ${peer.rssi}dBm", fontSize = 12.sp)
                            Text("Transport: ${peer.bestTransport.displayName}", fontSize = 12.sp)
                            Text("Trust: ${"%.0f".format(peer.reliability * 100)}%", fontSize = 12.sp)
                            Text("Battery: ${peer.batteryLevel}%", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
