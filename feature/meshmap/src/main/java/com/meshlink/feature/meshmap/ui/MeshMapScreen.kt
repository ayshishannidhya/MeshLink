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
package com.meshlink.feature.meshmap.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.core.ui.theme.MeshBlue
import com.meshlink.core.ui.theme.MeshGreen
import com.meshlink.core.ui.theme.SignalMedium
import com.meshlink.core.ui.theme.SignalWeak
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mesh topology visualization using Compose Canvas.
 *
 * Renders:
 * - Local device at center
 * - Nearby peers as nodes with signal-strength halos
 * - Edges showing active links with animated data flow
 * - Hop count labels
 * - Transport type color coding (BLE=blue, WiFi=green, LAN=amber)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshMapScreen(onNavigateBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Demo peer positions
    val demoNodes = remember {
        listOf(
            MeshNode("You", 0f, 0f, -30, "BLE", true),
            MeshNode("Peer A", -120f, -80f, -55, "BLE", true),
            MeshNode("Peer B", 100f, -120f, -70, "WiFi", true),
            MeshNode("Peer C", 140f, 60f, -85, "BLE", false),
            MeshNode("Peer D", -80f, 140f, -45, "LAN", true),
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Stats bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("Peers", "4")
                StatChip("Relays", "2")
                StatChip("Avg Hops", "1.5")
                StatChip("Coverage", "~200m")
            }

            // Canvas map
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Draw edges first (under nodes)
                demoNodes.drop(1).forEach { node ->
                    val nodeX = centerX + node.offsetX
                    val nodeY = centerY + node.offsetY
                    val edgeColor = when (node.transport) {
                        "BLE" -> MeshBlue
                        "WiFi" -> MeshGreen
                        else -> SignalMedium
                    }
                    drawLine(
                        color = edgeColor.copy(alpha = 0.4f),
                        start = Offset(centerX, centerY),
                        end = Offset(nodeX, nodeY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw center node pulse
                drawCircle(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(centerX, centerY)
                )

                // Draw nodes
                demoNodes.forEach { node ->
                    val nodeX = centerX + node.offsetX
                    val nodeY = centerY + node.offsetY
                    val signalColor = when {
                        node.rssi > -50 -> MeshGreen
                        node.rssi > -75 -> SignalMedium
                        else -> SignalWeak
                    }

                    // Signal halo
                    if (node.isActive) {
                        drawCircle(
                            color = signalColor.copy(alpha = 0.2f),
                            radius = 30f,
                            center = Offset(nodeX, nodeY)
                        )
                    }

                    // Node circle
                    drawCircle(
                        color = if (node.isActive) signalColor else Color.Gray,
                        radius = 16f,
                        center = Offset(nodeX, nodeY)
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 12f,
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(MeshBlue, "BLE")
                LegendItem(MeshGreen, "Wi-Fi Direct")
                LegendItem(SignalMedium, "LAN")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color, radius = 6f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private data class MeshNode(
    val name: String,
    val offsetX: Float,
    val offsetY: Float,
    val rssi: Int,
    val transport: String,
    val isActive: Boolean
)

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
