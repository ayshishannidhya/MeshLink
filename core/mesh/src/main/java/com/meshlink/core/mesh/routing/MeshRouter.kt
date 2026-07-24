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
 * Weighted mesh router implementing intelligent path selection.
 *
 * ## Scoring Algorithm
 * Each link is scored using 8 weighted metrics:
 * ```
 * score = wâ‚Â·RSSI + wâ‚‚Â·battery + wâ‚ƒÂ·latency + wâ‚„Â·packetLoss
 *       + wâ‚…Â·congestion + wâ‚†Â·reliability + wâ‚‡Â·hopStability + wâ‚ˆÂ·capability
 * ```
 *
 * Higher scores = better routes. The router picks the neighbor
 * with the highest composite score as the next hop.
 *
 * ## Design
 * - Uses neighbor table for 1-hop topology
 * - Source routes cached for known multi-hop paths
 * - Falls back to flooding when no route exists
 * - Routes expire after [MeshConstants.NEIGHBOR_FRESHNESS_MS]
 */
@Singleton
class MeshRouter @Inject constructor(
    private val neighborTable: NeighborTable
) {
    // Routing weights (tunable â€” could be adaptive in future)
    private val weights = RouteWeights()

    // Cached multi-hop routes: recipientId â†’ RouteEntry
    private val routeCache = LinkedHashMap<String, RouteEntry>(64, 0.75f, true)

    /**
     * Find the best route to a destination peer.
     *
     * @param recipientId Target peer's 8-byte ID
     * @return Route info with next hop, or null if no route known
     */
    fun findRoute(recipientId: ByteArray): RouteEntry? {
        // Check direct neighbor first
        val directNeighbor = neighborTable.getPeer(recipientId)
        if (directNeighbor != null && directNeighbor.isActive) {
            return RouteEntry(
                destinationId = recipientId,
                nextHopPeerId = recipientId,
                transport = directNeighbor.bestTransport,
                hopCount = 1,
                score = scoreLink(directNeighbor),
                timestamp = currentTimeMillis()
            )
        }

        // Check route cache
        val cacheKey = recipientId.toShortHex()
        val cached = routeCache[cacheKey]
        if (cached != null && !cached.isExpired()) {
            return cached
        }

        // No route â€” caller should flood
        return null
    }

    /**
     * Learn a route from a received packet (reverse path learning).
     */
    fun learnRoute(
        destinationId: ByteArray,
        nextHopPeerId: ByteArray,
        transport: TransportType,
        hopCount: Int
    ) {
        val key = destinationId.toShortHex()
        val existing = routeCache[key]

        // Only update if this is a better route
        val newEntry = RouteEntry(
            destinationId = destinationId,
            nextHopPeerId = nextHopPeerId,
            transport = transport,
            hopCount = hopCount,
            score = 1.0f / hopCount,  // Simple inverse hop score for learned routes
            timestamp = currentTimeMillis()
        )

        if (existing == null || newEntry.isBetterThan(existing)) {
            routeCache[key] = newEntry
            Timber.d("Learned route to ${destinationId.toShortHex()} via ${nextHopPeerId.toShortHex()} (${hopCount} hops)")
        }
    }

    /**
     * Score a direct neighbor link using all 8 metrics.
     */
    fun scoreLink(peer: NeighborEntry): Float {
        // Normalize RSSI: typical range -100 to -30 dBm â†’ 0.0 to 1.0
        val rssiNorm = ((peer.rssi + 100).toFloat() / 70f).coerceIn(0f, 1f)

        // Battery: 0-100% â†’ 0.0 to 1.0
        val batteryNorm = (peer.batteryLevel / 100f).coerceIn(0f, 1f)

        // Latency: lower is better. 0-500ms â†’ 1.0 to 0.0
        val latencyNorm = (1f - (peer.latencyMs / 500f)).coerceIn(0f, 1f)

        // Packet loss: lower is better. 0-100% â†’ 1.0 to 0.0
        val lossNorm = (1f - peer.packetLossRate).coerceIn(0f, 1f)

        // Congestion: lower is better
        val congestionNorm = (1f - peer.congestionLevel).coerceIn(0f, 1f)

        // Historical reliability (reputation): 0.0 to 1.0
        val reliabilityNorm = peer.reliability.coerceIn(0f, 1f)

        // Hop stability: how long the link has been alive
        val ageMs = currentTimeMillis() - peer.firstSeen
        val stabilityNorm = (ageMs.toFloat() / 300_000f).coerceIn(0f, 1f)  // 5 min = max

        // Device capability: higher is better (relay-capable, good hardware)
        val capabilityNorm = (peer.capabilities / 255f).coerceIn(0f, 1f)

        return weights.rssi * rssiNorm +
                weights.battery * batteryNorm +
                weights.latency * latencyNorm +
                weights.packetLoss * lossNorm +
                weights.congestion * congestionNorm +
                weights.reliability * reliabilityNorm +
                weights.hopStability * stabilityNorm +
                weights.capability * capabilityNorm
    }

    /** Clear expired routes from cache. */
    fun pruneExpiredRoutes() {
        routeCache.entries.removeAll { it.value.isExpired() }
    }
}

/**
 * Configurable routing weight vector.
 * Sum of all weights should be 1.0 for normalized scoring.
 */
data class RouteWeights(
    val rssi: Float = 0.20f,
    val battery: Float = 0.15f,
    val latency: Float = 0.15f,
    val packetLoss: Float = 0.15f,
    val congestion: Float = 0.10f,
    val reliability: Float = 0.10f,
    val hopStability: Float = 0.10f,
    val capability: Float = 0.05f
)

/**
 * A cached route entry.
 */
data class RouteEntry(
    val destinationId: ByteArray,
    val nextHopPeerId: ByteArray,
    val transport: TransportType,
    val hopCount: Int,
    val score: Float,
    val timestamp: Long
) {
    fun isExpired(): Boolean =
        currentTimeMillis() - timestamp > MeshConstants.NEIGHBOR_FRESHNESS_MS

    fun isBetterThan(other: RouteEntry): Boolean {
        if (isExpired()) return false
        if (other.isExpired()) return true
        return score > other.score || (score == other.score && hopCount < other.hopCount)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteEntry) return false
        return destinationId.contentEquals(other.destinationId)
    }

    override fun hashCode(): Int = destinationId.contentHashCode()
}
