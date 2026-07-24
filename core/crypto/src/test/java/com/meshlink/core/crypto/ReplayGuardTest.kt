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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for the replay protection sliding window.
 * Covers: sequential acceptance, duplicate rejection, out-of-order tolerance,
 * window sliding, and boundary conditions.
 */
class ReplayGuardTest {

    @Test
    fun `first nonce should be accepted`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(0)).isTrue()
    }

    @Test
    fun `sequential nonces should all be accepted`() {
        val guard = ReplayGuard()
        for (i in 0L..100L) {
            assertThat(guard.checkAndAccept(i)).isTrue()
        }
    }

    @Test
    fun `duplicate nonce should be rejected`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(42)).isTrue()
        assertThat(guard.checkAndAccept(42)).isFalse()
    }

    @Test
    fun `out of order within window should be accepted`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(10)).isTrue()
        assertThat(guard.checkAndAccept(8)).isTrue()  // Out of order but within window
        assertThat(guard.checkAndAccept(12)).isTrue()
        assertThat(guard.checkAndAccept(9)).isTrue()
    }

    @Test
    fun `out of order duplicate should be rejected`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(10)).isTrue()
        assertThat(guard.checkAndAccept(8)).isTrue()
        assertThat(guard.checkAndAccept(8)).isFalse()  // Duplicate
    }

    @Test
    fun `nonce too far behind window should be rejected`() {
        val guard = ReplayGuard(windowSize = 64)
        assertThat(guard.checkAndAccept(100)).isTrue()
        assertThat(guard.checkAndAccept(30)).isFalse()  // 70 behind, window is 64
    }

    @Test
    fun `nonce just within window edge should be accepted`() {
        val guard = ReplayGuard(windowSize = 64)
        assertThat(guard.checkAndAccept(100)).isTrue()
        assertThat(guard.checkAndAccept(37)).isTrue()  // 63 behind, within 64 window
    }

    @Test
    fun `negative nonce should be rejected`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(-1)).isFalse()
    }

    @Test
    fun `large jump forward should work`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(0)).isTrue()
        assertThat(guard.checkAndAccept(10000)).isTrue()
        assertThat(guard.checkAndAccept(0)).isFalse()  // Now too old
    }

    @Test
    fun `reset should clear all state`() {
        val guard = ReplayGuard()
        assertThat(guard.checkAndAccept(42)).isTrue()
        assertThat(guard.checkAndAccept(42)).isFalse()
        guard.reset()
        assertThat(guard.checkAndAccept(42)).isTrue()  // Accepted again after reset
    }

    @Test
    fun `wouldAccept should not modify state`() {
        val guard = ReplayGuard()
        assertThat(guard.wouldAccept(42)).isTrue()
        assertThat(guard.wouldAccept(42)).isTrue()  // Still true â€” state unchanged
        assertThat(guard.checkAndAccept(42)).isTrue()
        assertThat(guard.wouldAccept(42)).isFalse()  // Now false after actual accept
    }

    @Test
    fun `stress test with 10000 sequential nonces`() {
        val guard = ReplayGuard()
        for (i in 0L..10000L) {
            assertThat(guard.checkAndAccept(i)).isTrue()
        }
        // All old nonces should be rejected
        assertThat(guard.checkAndAccept(0)).isFalse()
        assertThat(guard.checkAndAccept(5000)).isFalse()
    }
}
