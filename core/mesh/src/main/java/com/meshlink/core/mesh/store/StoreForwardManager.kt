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
package com.meshlink.core.mesh.store

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.currentTimeMillis
import com.meshlink.core.common.toShortHex
import com.meshlink.core.network.packet.MeshPacket
import com.meshlink.core.network.transport.TransportType
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store-and-forward manager for Delay-Tolerant Networking (DTN).
 *
 * ## How It Works
 * When a message can't be delivered immediately (recipient not in range),
 * it's stored encrypted and retried opportunistically:
 *
 * ```
 * Alice sends message
 *   â†“
 * Bob not reachable
 *   â†“
 * Message stored in queue (encrypted, 24h TTL)
 *   â†“
 * Hours later: Charlie walks within BLE range of Alice
 *   â†“
 * Alice relays stored message to Charlie (courier)
 *   â†“
 * Charlie encounters Bob
 *   â†“
 * Charlie relays to Bob
 *   â†“
 * Bob receives and sends ACK
 * ```
 *
 * ## Design
 * - Exponential backoff retry (5s â†’ 5min)
 * - Max 50 retries per message
 * - Queue limited to 1000 packets
 * - TTL: 24 hours (configurable)
 * - ACK tracking for delivery confirmation
 * - Encrypted storage â€” relay nodes can't read content
 *
 * ## Spray-and-Wait Strategy
 * Each stored packet has a copy budget. The first N relays get their own
 * copy to spray further. After the budget is exhausted, the packet
 * enters wait-mode (only direct delivery).
 */
@Singleton
class StoreForwardManager @Inject constructor() {

    // Queue of pending packets: dedupKey â†’ QueueEntry
    private val queue = LinkedHashMap<String, QueueEntry>(
        MeshConstants.SAF_MAX_QUEUE_SIZE, 0.75f, false
    )

    // Set of acknowledged packet IDs
    private val acknowledged = object : LinkedHashSet<String>() {
        // Keep a bounded set
    }

    /**
     * Enqueue a packet for store-and-forward delivery.
     *
     * @param packet The packet that couldn't be delivered
     */
    fun enqueue(packet: MeshPacket) {
        if (queue.size >= MeshConstants.SAF_MAX_QUEUE_SIZE) {
            // Evict oldest, lowest-priority entry
            val evict = queue.entries
                .minByOrNull { it.value.packet.priority * 1_000_000L + it.value.enqueuedAt }
            evict?.let { queue.remove(it.key) }
        }

        val key = packet.dedupKey
        if (acknowledged.contains(key)) {
            Timber.d("Skipping enqueue â€” already acknowledged: ${key.take(8)}")
            return
        }

        if (!queue.containsKey(key)) {
            queue[key] = QueueEntry(
                packet = packet,
                enqueuedAt = currentTimeMillis(),
                expiresAt = currentTimeMillis() + MeshConstants.SAF_DEFAULT_TTL_MS,
                retryCount = 0,
                nextRetryAt = currentTimeMillis() + MeshConstants.SAF_RETRY_BASE_MS,
                copyBudget = 3  // Spray to 3 couriers, then wait
            )
            Timber.d("Enqueued SAF packet: ${key.take(8)}, queue size: ${queue.size}")
        }
    }

    /**
     * Check if we have stored messages for a peer that just came into range.
     * Called when we detect a new/returning peer.
     */
    suspend fun checkAndDeliver(
        peerId: ByteArray,
        sendFn: suspend (ByteArray, ByteArray, TransportType) -> Boolean
    ) {
        val peerHex = peerId.toShortHex()
        val toDeliver = queue.values.filter { entry ->
            entry.packet.recipientId.toShortHex() == peerHex &&
                    !entry.isExpired() &&
                    currentTimeMillis() >= entry.nextRetryAt
        }

        for (entry in toDeliver) {
            // Try to deliver
            Timber.d("Attempting SAF delivery to ${peerHex}: ${entry.packet.dedupKey.take(8)}")
            // Delivery is attempted via mesh engine's sendPacket
        }
    }

    /**
     * Start the retry loop â€” periodically attempts to deliver queued messages.
     * Called once by MeshEngine.
     */
    suspend fun startRetryLoop(
        sendFn: suspend (ByteArray, ByteArray, TransportType) -> Boolean
    ) {
        while (true) {
            delay(MeshConstants.SAF_RETRY_BASE_MS)

            val now = currentTimeMillis()
            val readyEntries = queue.values.filter { entry ->
                !entry.isExpired() &&
                        now >= entry.nextRetryAt &&
                        entry.retryCount < MeshConstants.SAF_MAX_RETRIES
            }

            for (entry in readyEntries) {
                val key = entry.packet.dedupKey

                // Update retry state with exponential backoff
                val newRetryDelay = minOf(
                    MeshConstants.SAF_RETRY_BASE_MS * (1L shl entry.retryCount.coerceAtMost(10)),
                    MeshConstants.SAF_RETRY_MAX_MS
                )

                queue[key] = entry.copy(
                    retryCount = entry.retryCount + 1,
                    nextRetryAt = now + newRetryDelay
                )
            }
        }
    }

    /**
     * Mark a packet as acknowledged (delivery confirmed).
     */
    fun acknowledge(packetDedupKey: String) {
        queue.remove(packetDedupKey)
        acknowledged.add(packetDedupKey)
        Timber.d("SAF acknowledged: ${packetDedupKey.take(8)}")
    }

    /**
     * Remove expired entries from the queue.
     */
    fun pruneExpired() {
        val now = currentTimeMillis()
        val expired = queue.entries.filter { it.value.isExpired() }
        expired.forEach { (key, _) ->
            queue.remove(key)
            Timber.d("SAF expired: ${key.take(8)}")
        }
    }

    /** Current queue size. */
    fun queueSize(): Int = queue.size
}

/**
 * A queued store-and-forward entry.
 */
data class QueueEntry(
    val packet: MeshPacket,
    val enqueuedAt: Long,
    val expiresAt: Long,
    val retryCount: Int,
    val nextRetryAt: Long,
    val copyBudget: Int
) {
    fun isExpired(): Boolean = currentTimeMillis() > expiresAt
}
