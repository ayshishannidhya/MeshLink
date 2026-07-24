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
package com.meshlink.core.mesh.routing

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.currentTimeMillis
import com.meshlink.core.common.toShortHex
import com.meshlink.core.network.transport.DiscoveredPeer
import com.meshlink.core.network.transport.TransportType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains a table of known mesh neighbors and their link quality.
 *
 * ## Design
 * - Updated by transport-layer peer discovery events
 * - Updated by received packets (contact proof)
 * - Entries expire after [MeshConstants.NEIGHBOR_FRESHNESS_MS] (60s)
 * - Tracks multi-transport links (a peer may be reachable via BLE + LAN)
 *
 * ## Thread Safety
 * Access is serialized by MeshEngine's single-threaded dispatcher.
 */
@Singleton
class NeighborTable @Inject constructor() {

    private val peers = LinkedHashMap<String, NeighborEntry>(32)

    /**
     * Update peer info from a transport discovery event.
     */
    fun updatePeer(discoveredPeer: DiscoveredPeer) {
        val key = discoveredPeer.peerId.toShortHex()
        val existing = peers[key]

        if (existing != null) {
            peers[key] = existing.copy(
                rssi = discoveredPeer.rssi ?: existing.rssi,
                lastSeen = currentTimeMillis(),
                transports = existing.transports + discoveredPeer.transport,
                capabilities = discoveredPeer.capabilities
            )
        } else {
            peers[key] = NeighborEntry(
                peerId = discoveredPeer.peerId,
                displayName = discoveredPeer.displayName,
                rssi = discoveredPeer.rssi ?: -80,
                firstSeen = currentTimeMillis(),
                lastSeen = currentTimeMillis(),
                transports = setOf(discoveredPeer.transport),
                capabilities = discoveredPeer.capabilities
            )
            Timber.d("New neighbor: ${key} via ${discoveredPeer.transport}")
        }
    }

    /**
     * Record direct contact with a peer (received a packet from them).
     */
    fun recordContact(peerId: ByteArray, transport: TransportType, rssi: Int?) {
        val key = peerId.toShortHex()
        val existing = peers[key]

        if (existing != null) {
            peers[key] = existing.copy(
                rssi = rssi ?: existing.rssi,
                lastSeen = currentTimeMillis(),
                transports = existing.transports + transport,
                packetsReceived = existing.packetsReceived + 1
            )
        } else {
            peers[key] = NeighborEntry(
                peerId = peerId,
                rssi = rssi ?: -80,
                firstSeen = currentTimeMillis(),
                lastSeen = currentTimeMillis(),
                transports = setOf(transport)
            )
        }
    }

    /**
     * Get a specific peer by ID.
     */
    fun getPeer(peerId: ByteArray): NeighborEntry? {
        return peers[peerId.toShortHex()]
    }

    /**
     * Get all currently active (non-expired) peers.
     */
    fun getActivePeers(): List<NeighborEntry> {
        val now = currentTimeMillis()
        return peers.values.filter {
            now - it.lastSeen < MeshConstants.NEIGHBOR_FRESHNESS_MS
        }
    }

    /**
     * Remove stale entries (older than freshness window).
     */
    fun pruneStale() {
        val now = currentTimeMillis()
        val stale = peers.entries.filter {
            now - it.value.lastSeen > MeshConstants.NEIGHBOR_FRESHNESS_MS * 2
        }
        stale.forEach { (key, entry) ->
            peers.remove(key)
            Timber.d("Pruned stale neighbor: ${entry.peerId.toShortHex()}")
        }
    }

    /**
     * Report successful packet delivery to a peer (for reputation).
     */
    fun reportDeliverySuccess(peerId: ByteArray) {
        val key = peerId.toShortHex()
        peers[key]?.let {
            peers[key] = it.copy(
                reliability = (it.reliability + MeshConstants.REPUTATION_RELAY_REWARD)
                    .coerceAtMost(MeshConstants.REPUTATION_MAX)
            )
        }
    }

    /**
     * Report failed delivery to a peer (for reputation).
     */
    fun reportDeliveryFailure(peerId: ByteArray) {
        val key = peerId.toShortHex()
        peers[key]?.let {
            peers[key] = it.copy(
                reliability = (it.reliability - MeshConstants.REPUTATION_RELAY_PENALTY)
                    .coerceAtLeast(MeshConstants.REPUTATION_MIN)
            )
        }
    }

    /** Total number of tracked peers (including stale). */
    val size: Int get() = peers.size
}

/**
 * Represents a known mesh neighbor and its link quality metrics.
 */
data class NeighborEntry(
    val peerId: ByteArray,
    val displayName: String? = null,
    val rssi: Int = -80,
    val batteryLevel: Int = 100,
    val latencyMs: Long = 50,
    val packetLossRate: Float = 0f,
    val congestionLevel: Float = 0f,
    val reliability: Float = MeshConstants.REPUTATION_INITIAL,
    val capabilities: Int = 0,
    val firstSeen: Long = 0,
    val lastSeen: Long = 0,
    val transports: Set<TransportType> = emptySet(),
    val packetsReceived: Long = 0
) {
    /** Whether this peer is considered active (seen recently). */
    val isActive: Boolean
        get() = currentTimeMillis() - lastSeen < MeshConstants.NEIGHBOR_FRESHNESS_MS

    /** Best available transport for this peer (by priority). */
    val bestTransport: TransportType
        get() = transports.minByOrNull { it.ordinal } ?: TransportType.BLE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NeighborEntry) return false
        return peerId.contentEquals(other.peerId)
    }

    override fun hashCode(): Int = peerId.contentHashCode()
}
