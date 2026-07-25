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
package com.meshlink.core.domain.model

import com.meshlink.core.network.transport.TransportType

data class MeshTopology(
    val localNode: TopologyNode,
    val peerNodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val stats: MeshMapStats,
    val health: NetworkHealth
)

data class TopologyNode(
    val id: String,
    val displayName: String,
    val angle: Float,
    val distance: Float,
    val rssi: Int,
    val transport: TransportType,
    val isActive: Boolean,
    val reliability: Float
)

data class TopologyEdge(
    val fromId: String,
    val toId: String,
    val transport: TransportType,
    val strength: Float
)

data class MeshMapStats(
    val nearbyDevices: Int,
    val activeRoutes: Int,
    val messageQueue: Int,
    val networkHealth: NetworkHealth
)

enum class NetworkHealth(val label: String, val emoji: String) {
    EXCELLENT("Excellent", "🟢"),
    GOOD("Good", "🟢"),
    WEAK("Weak", "🟡"),
    CRITICAL("Critical", "🔴"),
    NO_PEERS("No Network", "⚫")
}
