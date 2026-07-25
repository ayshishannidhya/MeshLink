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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.core.domain.model.MeshTopology
import com.meshlink.core.domain.model.TopologyNode
import com.meshlink.core.network.transport.TransportType
import com.meshlink.core.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshMapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MeshMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (uiState is MeshMapUiState.Active) {
                        val health = (uiState as MeshMapUiState.Active).topology.health
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = when(health.name) {
                                            "EXCELLENT", "GOOD" -> MeshGreen
                                            "WEAK" -> MeshAmber
                                            "CRITICAL" -> MeshRed
                                            else -> Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${health.emoji} ${health.label}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MeshMapUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading mesh data...")
                    }
                }
                is MeshMapUiState.Empty -> {
                    EmptyMeshState()
                }
                is MeshMapUiState.Active -> {
                    ActiveMeshMap(topology = state.topology)
                }
            }
        }
    }
}

@Composable
fun EmptyMeshState() {
    val transition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = radius * 0.66f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = radius * 0.33f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Sweep
            drawArc(
                color = primaryColor.copy(alpha = 0.3f),
                startAngle = sweepAngle,
                sweepAngle = 45f,
                useCenter = true
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No peers in range. Enable Bluetooth and move closer to other MeshLink devices.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class NodeAnimData(
    val node: TopologyNode,
    val distance: Float,
    val angle: Float
)

@Composable
fun ActiveMeshMap(topology: MeshTopology) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip("Nearby", topology.stats.nearbyDevices.toString())
            StatChip("Routes", topology.stats.activeRoutes.toString())
            StatChip("Queue", topology.stats.messageQueue.toString())
        }

        // Animated states
        val textMeasurer = rememberTextMeasurer()
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        
        val pulseRing1 by infiniteTransition.animateFloat(
            initialValue = 30f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse1"
        )
        val alphaRing1 by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha1"
        )
        
        val pulseRing2 by infiniteTransition.animateFloat(
            initialValue = 30f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse2"
        )
        val alphaRing2 by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha2"
        )

        val nodePositions = topology.peerNodes.map { node ->
            key(node.id) {
                val animDistance by animateFloatAsState(
                    targetValue = node.distance,
                    animationSpec = tween(800),
                    label = "distance_${node.id}"
                )
                val animAngle by animateFloatAsState(
                    targetValue = node.angle,
                    animationSpec = tween(800),
                    label = "angle_${node.id}"
                )
                NodeAnimData(node, animDistance, animAngle)
            }
        }

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Network mesh map visualization showing ${topology.peerNodes.size} peers" }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f
                val maxRadius = min(canvasWidth, canvasHeight) * 0.38f

                // Draw edges first
                for (edge in topology.edges) {
                    val targetNodePos = nodePositions.find { it.node.id == edge.toId }
                    if (targetNodePos != null && edge.fromId == topology.localNode.id) {
                        val edgeColor = when (edge.transport) {
                            TransportType.BLE -> MeshBlue
                            TransportType.WIFI_DIRECT -> MeshGreen
                            TransportType.LAN -> SignalMedium
                        }
                        
                        val x = centerX + targetNodePos.distance * maxRadius * cos(targetNodePos.angle)
                        val y = centerY + targetNodePos.distance * maxRadius * sin(targetNodePos.angle)
                        
                        drawLine(
                            color = edgeColor.copy(alpha = edge.strength),
                            start = Offset(centerX, centerY),
                            end = Offset(x, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                // Draw local node with pulse rings
                drawCircle(
                    color = MeshGreen.copy(alpha = alphaRing1),
                    radius = pulseRing1.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = MeshGreen.copy(alpha = alphaRing2),
                    radius = pulseRing2.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                
                // Local node inner circle
                drawCircle(
                    color = MeshGreen,
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = topology.localNode.displayName,
                    topLeft = Offset(centerX - 20.dp.toPx(), centerY + 18.dp.toPx()),
                    style = TextStyle(color = onSurfaceColor, fontSize = 12.sp)
                )

                // Draw peer nodes
                for (nodePos in nodePositions) {
                    val node = nodePos.node
                    val x = centerX + nodePos.distance * maxRadius * cos(nodePos.angle)
                    val y = centerY + nodePos.distance * maxRadius * sin(nodePos.angle)
                    
                    if (node.isActive) {
                        val signalColor = when {
                            node.rssi > -50 -> SignalStrong
                            node.rssi > -75 -> SignalMedium
                            else -> SignalWeak
                        }
                        // Halo
                        drawCircle(
                            color = signalColor.copy(alpha = 0.2f),
                            radius = 16.dp.toPx(),
                            center = Offset(x, y)
                        )
                        // Filled
                        drawCircle(
                            color = signalColor,
                            radius = 8.dp.toPx(),
                            center = Offset(x, y)
                        )
                    } else {
                        drawCircle(
                            color = Color.Gray,
                            radius = 8.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = node.displayName,
                        topLeft = Offset(x - 20.dp.toPx(), y + 18.dp.toPx()),
                        style = TextStyle(color = onSurfaceColor, fontSize = 12.sp)
                    )
                }
            }
        }

        // Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("BLE", MeshBlue)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem("Wi-Fi Direct", MeshGreen)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem("LAN", SignalMedium)
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.semantics { contentDescription = "$label: $value" }
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
