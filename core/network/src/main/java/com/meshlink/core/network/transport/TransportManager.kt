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
package com.meshlink.core.network.transport

import com.meshlink.core.common.toShortHex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Manages multiple transports and provides unified packet routing.
 *
 * ## Priority-Based Transport Selection
 * ```
 * BLE Mesh (always on, lowest power)
 *   â†“ fallback
 * Wi-Fi Direct (high bandwidth, moderate power)
 *   â†“ fallback
 * Local LAN (requires shared network)
 * ```
 *
 * No internet relay in V1 â€” this is an offline-first platform.
 *
 * ## Responsibilities
 * - Start/stop all transports
 * - Merge incoming packet streams
 * - Route outgoing packets through best available transport
 * - Monitor transport health
 * - Automatic failover when a transport goes down
 */
class TransportManager(
    private val transports: Set<MeshTransport>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Merged stream of incoming packets from all transports. */
    val incomingPackets: Flow<TransportPacket> = transports
        .map { it.incomingPackets }
        .merge()

    /** Merged stream of discovered peers from all transports. */
    val discoveredPeers: Flow<DiscoveredPeer> = transports
        .map { it.discoveredPeers }
        .merge()

    /** Start all transports. */
    suspend fun startAll() {
        transports.forEach { transport ->
            scope.launch {
                try {
                    transport.start()
                    Timber.i("Transport ${transport.transportType} started")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start ${transport.transportType}")
                }
            }
        }
    }

    /** Stop all transports. */
    suspend fun stopAll() {
        transports.forEach { transport ->
            try {
                transport.stop()
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop ${transport.transportType}")
            }
        }
        scope.cancel()
    }

    /**
     * Send packet through the best available transport for a given peer.
     * Falls through priority order: BLE â†’ Wi-Fi Direct â†’ LAN.
     */
    suspend fun sendPacket(
        peerId: ByteArray?,
        data: ByteArray,
        preferredTransport: TransportType? = null
    ): Boolean {
        // Try preferred transport first
        if (preferredTransport != null) {
            val transport = transports.find {
                it.transportType == preferredTransport && it.isActive
            }
            if (transport != null && transport.sendPacket(peerId, data)) {
                return true
            }
        }

        // Fallback through priority order
        val orderedTransports = transports
            .filter { it.isActive }
            .sortedBy { it.transportType.ordinal }

        for (transport in orderedTransports) {
            try {
                if (transport.sendPacket(peerId, data)) {
                    return true
                }
            } catch (e: Exception) {
                Timber.w("Send failed on ${transport.transportType}: ${e.message}")
            }
        }

        Timber.w("All transports failed for peer ${peerId?.toShortHex() ?: "broadcast"}")
        return false
    }

    /** Get health metrics for all transports. */
    fun getHealthReport(): Map<TransportType, TransportHealth> {
        return transports.associate { it.transportType to it.getHealthMetrics() }
    }

    /** Check if any transport is active. */
    fun hasActiveTransport(): Boolean = transports.any { it.isActive }
}

/**
 * Flow.merge extension for a collection of flows.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> Iterable<Flow<T>>.merge(): Flow<T> = channelFlow {
    forEach { flow ->
        launch {
            flow.collect { send(it) }
        }
    }
}
