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
package com.meshlink.core.mesh.flood

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.currentTimeMillis
import com.meshlink.core.common.toShortHex
import com.meshlink.core.mesh.routing.NeighborEntry
import com.meshlink.core.network.packet.MeshPacket
import com.meshlink.core.network.packet.PacketType
import com.meshlink.core.network.transport.TransportType
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Controlled flood mechanism for mesh packet propagation.
 *
 * ## Flood Control Mechanisms
 *
 * ### 1. Deduplication (LRU seen-set)
 * - 2000-entry LRU cache keyed by [senderId + packetId]
 * - 5-minute expiry â€” rejects identical packets
 *
 * ### 2. TTL (Time-To-Live)
 * - Packets originate with TTL 7
 * - Dense graphs (â‰¥6 links) cap broadcast TTL at 5
 * - Thin chains (â‰¤2 links) relay at full incoming TTL
 *
 * ### 3. Random Jitter
 * - Relays wait 20-250ms (random, wider when dense)
 * - Allows duplicate suppression to win â€” reduces redundant transmissions
 *
 * ### 4. Fanout Subsetting
 * - Broadcasts sent to logâ‚‚(degree) randomly-selected neighbors
 * - Emergency/announce packets use full fanout
 * - Ingress link always excluded (split horizon)
 *
 * ### 5. Priority Preemption
 * - Emergency (priority 0) packets bypass jitter
 * - Coordinator (priority 1) packets get reduced jitter
 */
@Singleton
class FloodController @Inject constructor() {

    // LRU dedup cache: dedupKey â†’ timestamp
    private val seenCache = object : LinkedHashMap<String, Long>(
        MeshConstants.DEDUP_CACHE_SIZE, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > MeshConstants.DEDUP_CACHE_SIZE
        }
    }

    // Pending relay jobs (can be cancelled if duplicate arrives)
    private val pendingRelays = mutableMapOf<String, Job>()

    /**
     * Check if a packet should be processed (not a duplicate).
     * Marks it as seen if accepting.
     */
    fun shouldProcess(packet: MeshPacket): Boolean {
        val key = packet.dedupKey
        val now = currentTimeMillis()

        val lastSeen = seenCache[key]
        if (lastSeen != null && now - lastSeen < MeshConstants.DEDUP_EXPIRY_MS) {
            // Duplicate â€” also cancel any pending relay for this packet
            pendingRelays.remove(key)?.cancel()
            return false
        }

        seenCache[key] = now
        return true
    }

    /**
     * Mark a packet as seen (for outgoing packets to prevent self-relay).
     */
    fun markSeen(packet: MeshPacket) {
        seenCache[packet.dedupKey] = currentTimeMillis()
    }

    /**
     * Schedule a relay with random jitter.
     * If a duplicate arrives before jitter expires, the relay is cancelled.
     *
     * @param packet The packet to relay (TTL already decremented)
     * @param ingressPeerId The peer we received it from (excluded from fanout)
     * @param relayAction The actual send operation
     */
    fun scheduleRelay(
        packet: MeshPacket,
        ingressPeerId: ByteArray?,
        relayAction: suspend (MeshPacket) -> Unit
    ) {
        val key = packet.dedupKey

        // Emergency packets bypass jitter
        if (packet.priority == MeshConstants.PRIORITY_EMERGENCY) {
            CoroutineScope(Dispatchers.Default).launch {
                relayAction(packet)
            }
            return
        }

        // Calculate jitter based on priority and density
        val jitterMs = calculateJitter(packet.priority)

        val job = CoroutineScope(Dispatchers.Default).launch {
            delay(jitterMs)
            // If we haven't been cancelled by a duplicate arriving, relay
            if (isActive) {
                relayAction(packet)
                Timber.v("Relayed packet ${key.take(8)} after ${jitterMs}ms jitter")
            }
        }

        pendingRelays[key] = job
    }

    /**
     * Select which neighbors to forward a broadcast to.
     *
     * Uses fanout subsetting: sends to logâ‚‚(degree) randomly-chosen peers
     * for regular broadcasts, but full fanout for emergency/announce packets.
     *
     * @param activePeers All currently reachable neighbors
     * @param packet The packet being forwarded
     * @param excludePeerId Split-horizon: exclude the peer we received from
     * @return List of (peerId, transport) to send to
     */
    fun selectFanoutTargets(
        activePeers: List<NeighborEntry>,
        packet: MeshPacket,
        excludePeerId: ByteArray?
    ): List<Pair<ByteArray, TransportType>> {
        // Filter out ingress peer (split horizon)
        val candidates = if (excludePeerId != null) {
            activePeers.filter { !it.peerId.contentEquals(excludePeerId) }
        } else {
            activePeers
        }

        if (candidates.isEmpty()) return emptyList()

        // Full fanout for emergency, announce, and sync packets
        val fullFanoutTypes = setOf(
            PacketType.SOS,
            PacketType.ANNOUNCE,
            PacketType.SYNC_REQUEST,
            PacketType.NOISE_HANDSHAKE
        )

        if (packet.type in fullFanoutTypes || packet.priority <= MeshConstants.PRIORITY_COORDINATOR) {
            return candidates.map { it.peerId to it.bestTransport }
        }

        // Fanout subsetting: send to logâ‚‚(degree) peers
        val degree = candidates.size
        val fanout = if (degree <= 3) {
            degree  // Small network â€” send to all
        } else {
            (ln(degree.toDouble()) / ln(2.0)).roundToInt().coerceAtLeast(2)
        }

        // Select highest-scored peers for better reliability
        return candidates
            .shuffled()
            .take(fanout)
            .map { it.peerId to it.bestTransport }
    }

    /**
     * Apply TTL clamping based on local network density.
     *
     * @param originalTtl The TTL from the incoming packet
     * @param localDegree Number of currently connected neighbors
     * @return Adjusted TTL for relaying
     */
    fun clampTtl(originalTtl: Int, localDegree: Int): Int {
        return when {
            localDegree >= MeshConstants.DENSE_THRESHOLD -> minOf(originalTtl, 5)
            localDegree <= 2 -> originalTtl  // Thin chain â€” relay fully
            else -> originalTtl
        }
    }

    /** Remove expired entries from the dedup cache. */
    fun pruneDedup() {
        val now = currentTimeMillis()
        seenCache.entries.removeAll { now - it.value > MeshConstants.DEDUP_EXPIRY_MS }
        pendingRelays.entries.removeAll { !it.value.isActive }
    }

    private fun calculateJitter(priority: Int): Long {
        val (min, max) = when (priority) {
            MeshConstants.PRIORITY_EMERGENCY -> 0L to 0L
            MeshConstants.PRIORITY_COORDINATOR -> 10L to 50L
            MeshConstants.PRIORITY_MESSAGE -> MeshConstants.RELAY_JITTER_MIN_MS to MeshConstants.RELAY_JITTER_MAX_MS
            else -> MeshConstants.RELAY_JITTER_MIN_MS to MeshConstants.RELAY_JITTER_MAX_MS * 2
        }
        return if (max <= min) min else Random.nextLong(min, max)
    }
}
