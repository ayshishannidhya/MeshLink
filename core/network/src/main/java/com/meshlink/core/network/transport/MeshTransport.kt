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

import com.meshlink.core.network.packet.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Abstract transport interface for mesh communication.
 *
 * Each transport implementation (BLE, Wi-Fi Direct, LAN) conforms
 * to this contract, enabling the [TransportManager] to treat them
 * uniformly and failover between them.
 *
 * ## Lifecycle
 * 1. [start] â€” Begin advertising/scanning for peers
 * 2. Observe [discoveredPeers] for new neighbors
 * 3. [sendPacket] to transmit data
 * 4. Observe [incomingPackets] for received data
 * 5. [stop] â€” Clean shutdown
 *
 * ## Design
 * - All methods are suspend functions for coroutine integration
 * - Peer discovery and packet reception use Kotlin Flow
 * - Each transport reports its own [TransportType] and health metrics
 */
interface MeshTransport {

    /** The type of this transport. */
    val transportType: TransportType

    /** Whether this transport is currently active. */
    val isActive: Boolean

    /** Flow of incoming packets from this transport. */
    val incomingPackets: SharedFlow<TransportPacket>

    /** Flow of discovered peers through this transport. */
    val discoveredPeers: Flow<DiscoveredPeer>

    /** Start the transport (advertising + scanning). */
    suspend fun start()

    /** Stop the transport gracefully. */
    suspend fun stop()

    /**
     * Send a packet via this transport.
     *
     * @param peerId Target peer ID (for directed) or null (for broadcast)
     * @param data Raw packet bytes
     * @return true if sent successfully
     */
    suspend fun sendPacket(peerId: ByteArray?, data: ByteArray): Boolean

    /**
     * Get current health metrics for this transport.
     */
    fun getHealthMetrics(): TransportHealth
}

/**
 * Supported transport types in priority order.
 * Lower ordinal = higher priority.
 */
enum class TransportType(val displayName: String) {
    BLE("Bluetooth LE"),
    WIFI_DIRECT("Wi-Fi Direct"),
    LAN("Local Network");
    // INTERNET intentionally excluded from V1 â€” offline first
}

/**
 * A packet received from a specific transport.
 */
data class TransportPacket(
    val data: ByteArray,
    val fromPeerId: ByteArray?,
    val transport: TransportType,
    val rssi: Int? = null  // BLE only
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportPacket) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int = data.contentHashCode()
}

/**
 * A peer discovered by a transport layer.
 */
data class DiscoveredPeer(
    val peerId: ByteArray,
    val transport: TransportType,
    val rssi: Int? = null,
    val displayName: String? = null,
    val capabilities: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiscoveredPeer) return false
        return peerId.contentEquals(other.peerId)
    }

    override fun hashCode(): Int = peerId.contentHashCode()
}

/**
 * Health metrics for a transport.
 */
data class TransportHealth(
    val isConnected: Boolean = false,
    val peerCount: Int = 0,
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsDropped: Long = 0,
    val averageLatencyMs: Long = 0,
    val estimatedBandwidthBps: Long = 0,
    val lastActivityTimestamp: Long = 0
)
