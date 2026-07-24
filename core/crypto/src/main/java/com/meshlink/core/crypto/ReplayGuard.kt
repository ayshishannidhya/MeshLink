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
package com.meshlink.core.crypto

import com.meshlink.core.common.MeshConstants

/**
 * Sliding-window replay protection for encrypted sessions.
 *
 * ## How It Works
 * Maintains a bitmap of recently-seen nonces relative to the highest
 * received nonce. This allows:
 * - Detection of replayed (duplicate) messages
 * - Tolerance for out-of-order delivery (within window)
 * - Rejection of messages with ancient nonces
 *
 * ## Window Size
 * 2048 bits (256 bytes) â€” supports up to 2048 out-of-order messages.
 * This is generous for BLE mesh where reordering is common due to
 * multi-path routing and relay jitter.
 *
 * ## Algorithm
 * Based on RFC 6479 (IPsec sliding window) adapted for Noise nonces.
 *
 * ## Thread Safety
 * NOT thread-safe â€” callers must synchronize externally.
 * In practice, each NoiseSession has its own ReplayGuard.
 */
class ReplayGuard(
    private val windowSize: Int = MeshConstants.REPLAY_WINDOW_SIZE
) {
    private val windowBytes = windowSize / 8
    private var highestNonce: Long = -1
    private val window: ByteArray = ByteArray(windowBytes)

    /**
     * Check if a nonce is valid (not replayed, not too old).
     *
     * @param nonce The received nonce value
     * @return true if the nonce should be accepted, false if replayed
     */
    fun checkAndAccept(nonce: Long): Boolean {
        if (nonce < 0) return false

        // First message ever
        if (highestNonce < 0) {
            highestNonce = nonce
            markBit(0)
            return true
        }

        val diff = nonce - highestNonce

        if (diff > 0) {
            // New highest nonce â€” shift window
            if (diff >= windowSize) {
                // Complete window reset
                window.fill(0)
            } else {
                shiftWindow(diff.toInt())
            }
            highestNonce = nonce
            markBit(0)
            return true
        }

        if (diff == 0L) {
            // Exact duplicate
            return false
        }

        // Nonce is older than highest
        val offset = (-diff).toInt()
        if (offset >= windowSize) {
            // Too old â€” outside window
            return false
        }

        // Check if already seen
        if (isBitSet(offset)) {
            return false
        }

        // Mark as seen and accept
        markBit(offset)
        return true
    }

    /**
     * Check without accepting â€” for preview/validation.
     */
    fun wouldAccept(nonce: Long): Boolean {
        if (nonce < 0) return false
        if (highestNonce < 0) return true

        val diff = nonce - highestNonce
        if (diff > 0) return true
        if (diff == 0L) return false

        val offset = (-diff).toInt()
        if (offset >= windowSize) return false

        return !isBitSet(offset)
    }

    /** Reset the guard (e.g., for new session). */
    fun reset() {
        highestNonce = -1
        window.fill(0)
    }

    private fun isBitSet(offset: Int): Boolean {
        val byteIdx = offset / 8
        val bitIdx = offset % 8
        return (window[byteIdx].toInt() and (1 shl bitIdx)) != 0
    }

    private fun markBit(offset: Int) {
        val byteIdx = offset / 8
        val bitIdx = offset % 8
        window[byteIdx] = (window[byteIdx].toInt() or (1 shl bitIdx)).toByte()
    }

    private fun shiftWindow(shift: Int) {
        if (shift >= windowSize) {
            window.fill(0)
            return
        }

        val byteShift = shift / 8
        val bitShift = shift % 8

        // Shift bytes from right to left
        for (i in windowBytes - 1 downTo 0) {
            val sourceIdx = i - byteShift
            var newByte = 0

            if (sourceIdx >= 0) {
                newByte = (window[sourceIdx].toInt() and 0xFF) ushr bitShift
                if (sourceIdx > 0 && bitShift != 0) {
                    newByte = newByte or ((window[sourceIdx - 1].toInt() and 0xFF) shl (8 - bitShift))
                }
            }

            window[i] = (newByte and 0xFF).toByte()
        }
    }
}
