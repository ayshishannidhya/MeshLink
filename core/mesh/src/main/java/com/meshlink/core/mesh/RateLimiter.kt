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
package com.meshlink.core.mesh

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.currentTimeMillis
import com.meshlink.core.common.toShortHex
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-peer rate limiter to prevent flood/spam attacks on the mesh.
 *
 * ## Strategy
 * Sliding window rate limiting: each peer is allowed
 * [MeshConstants.RATE_LIMIT_MAX_PACKETS] packets per
 * [MeshConstants.RATE_LIMIT_WINDOW_MS] (default: 120 packets/minute).
 *
 * ## Why This Matters
 * Without rate limiting, a single malicious device could overwhelm the
 * entire mesh by flooding packets. This is especially dangerous in BLE
 * where bandwidth is ~25KB/s shared among all connections.
 *
 * ## Design
 * - O(1) check per packet
 * - Automatic cleanup of stale entries
 * - Emergency packets exempt from rate limiting
 */
@Singleton
class RateLimiter @Inject constructor() {

    // peerId hex â†’ list of timestamps of recent packets
    private val windows = mutableMapOf<String, MutableList<Long>>()

    /**
     * Check if a packet from this peer should be allowed.
     *
     * @param senderId Peer's 8-byte ID
     * @return true if within rate limits, false if should be dropped
     */
    fun allowPacket(senderId: ByteArray): Boolean {
        val key = senderId.toShortHex()
        val now = currentTimeMillis()
        val windowStart = now - MeshConstants.RATE_LIMIT_WINDOW_MS

        val timestamps = windows.getOrPut(key) { mutableListOf() }

        // Remove timestamps outside the window
        timestamps.removeAll { it < windowStart }

        if (timestamps.size >= MeshConstants.RATE_LIMIT_MAX_PACKETS) {
            Timber.w("Rate limit exceeded for peer $key: ${timestamps.size} packets in window")
            return false
        }

        timestamps.add(now)
        return true
    }

    /**
     * Clean up stale entries from peers no longer active.
     */
    fun cleanup() {
        val now = currentTimeMillis()
        val windowStart = now - MeshConstants.RATE_LIMIT_WINDOW_MS
        windows.entries.removeAll { (_, timestamps) ->
            timestamps.removeAll { it < windowStart }
            timestamps.isEmpty()
        }
    }
}
