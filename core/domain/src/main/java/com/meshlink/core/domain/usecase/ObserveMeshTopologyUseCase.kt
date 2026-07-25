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
package com.meshlink.core.domain.usecase

import com.meshlink.core.common.toShortHex
import com.meshlink.core.domain.model.*
import com.meshlink.core.domain.repository.MeshRepository
import com.meshlink.core.network.transport.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.absoluteValue

class ObserveMeshTopologyUseCase @Inject constructor(
    private val meshRepository: MeshRepository,
    private val calculateNetworkHealthUseCase: CalculateNetworkHealthUseCase
) {
    operator fun invoke(): Flow<MeshTopology> {
        return combine(
            meshRepository.activePeers,
            meshRepository.meshStats
        ) { peers, stats ->
            val health = calculateNetworkHealthUseCase(peers, stats)
            
            val localNode = TopologyNode(
                id = "local",
                displayName = "Me",
                angle = 0f,
                distance = 0f,
                rssi = 0,
                transport = TransportType.BLE,
                isActive = true,
                reliability = 1f
            )

            val peerNodes = peers.map { peer ->
                val idStr = peer.peerId.toShortHex()
                val angle = (idStr.hashCode().absoluteValue % 360) * (PI / 180).toFloat()
                
                // Map RSSI [-100, -30] to [0.85f, 0.2f]
                val normalizedRssi = (peer.rssi.coerceIn(-100, -30) + 100) / 70f
                val distance = 0.85f - (normalizedRssi * 0.65f)

                TopologyNode(
                    id = idStr,
                    displayName = peer.displayName ?: "Unknown",
                    angle = angle,
                    distance = distance,
                    rssi = peer.rssi,
                    transport = peer.bestTransport,
                    isActive = peer.isActive,
                    reliability = peer.reliability
                )
            }

            val edges = peerNodes.map { node ->
                TopologyEdge(
                    fromId = "local",
                    toId = node.id,
                    transport = node.transport,
                    strength = node.reliability
                )
            }

            val meshStats = MeshMapStats(
                nearbyDevices = peers.size,
                activeRoutes = peers.count { it.isActive },
                messageQueue = stats.pendingMessages,
                networkHealth = health
            )

            MeshTopology(
                localNode = localNode,
                peerNodes = peerNodes,
                edges = edges,
                stats = meshStats,
                health = health
            )
        }
    }
}
