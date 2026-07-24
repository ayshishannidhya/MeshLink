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

import com.google.common.truth.Truth.assertThat
import com.meshlink.core.common.MeshConstants
import org.junit.Before
import org.junit.Test

/**
 * Tests for the sliding window rate limiter.
 */
class RateLimiterTest {

    private lateinit var rateLimiter: RateLimiter

    @Before
    fun setup() {
        rateLimiter = RateLimiter()
    }

    @Test
    fun `first packet should be allowed`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        assertThat(rateLimiter.allowPacket(peerId)).isTrue()
    }

    @Test
    fun `packets within limit should all be allowed`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        repeat(MeshConstants.RATE_LIMIT_MAX_PACKETS - 1) {
            assertThat(rateLimiter.allowPacket(peerId)).isTrue()
        }
    }

    @Test
    fun `packet exceeding limit should be rejected`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        repeat(MeshConstants.RATE_LIMIT_MAX_PACKETS) {
            rateLimiter.allowPacket(peerId)
        }
        assertThat(rateLimiter.allowPacket(peerId)).isFalse()
    }

    @Test
    fun `different peers have independent limits`() {
        val peer1 = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0)
        val peer2 = byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0)

        repeat(MeshConstants.RATE_LIMIT_MAX_PACKETS) {
            rateLimiter.allowPacket(peer1)
        }
        // peer1 is rate limited
        assertThat(rateLimiter.allowPacket(peer1)).isFalse()
        // peer2 should still be allowed
        assertThat(rateLimiter.allowPacket(peer2)).isTrue()
    }

    @Test
    fun `cleanup removes empty windows`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        rateLimiter.allowPacket(peerId)
        // Cleanup should not crash
        rateLimiter.cleanup()
    }
}
