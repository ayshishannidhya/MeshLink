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

import com.google.common.truth.Truth.assertThat
import com.meshlink.core.network.transport.DiscoveredPeer
import com.meshlink.core.network.transport.TransportType
import org.junit.Before
import org.junit.Test

/**
 * Tests for the neighbor table and mesh router.
 */
class MeshRouterTest {

    private lateinit var neighborTable: NeighborTable
    private lateinit var router: MeshRouter

    @Before
    fun setup() {
        neighborTable = NeighborTable()
        router = MeshRouter(neighborTable)
    }

    @Test
    fun `direct neighbor should have route`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        neighborTable.updatePeer(
            DiscoveredPeer(
                peerId = peerId,
                transport = TransportType.BLE,
                rssi = -50
            )
        )

        val route = router.findRoute(peerId)
        assertThat(route).isNotNull()
        assertThat(route!!.hopCount).isEqualTo(1)
        assertThat(route.transport).isEqualTo(TransportType.BLE)
    }

    @Test
    fun `unknown peer should have no route`() {
        val unknownPeer = byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9)
        val route = router.findRoute(unknownPeer)
        assertThat(route).isNull()
    }

    @Test
    fun `learned route should be cached`() {
        val destId = byteArrayOf(5, 5, 5, 5, 5, 5, 5, 5)
        val nextHop = byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1)

        router.learnRoute(destId, nextHop, TransportType.BLE, hopCount = 3)

        val route = router.findRoute(destId)
        assertThat(route).isNotNull()
        assertThat(route!!.hopCount).isEqualTo(3)
    }

    @Test
    fun `better route should replace worse route`() {
        val destId = byteArrayOf(5, 5, 5, 5, 5, 5, 5, 5)
        val hop1 = byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
        val hop2 = byteArrayOf(2, 2, 2, 2, 2, 2, 2, 2)

        router.learnRoute(destId, hop1, TransportType.BLE, hopCount = 5)
        router.learnRoute(destId, hop2, TransportType.BLE, hopCount = 2)

        val route = router.findRoute(destId)
        assertThat(route).isNotNull()
        assertThat(route!!.hopCount).isEqualTo(2)
    }

    @Test
    fun `neighbor table tracks multiple transports`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        neighborTable.updatePeer(
            DiscoveredPeer(peerId = peerId, transport = TransportType.BLE, rssi = -50)
        )
        neighborTable.updatePeer(
            DiscoveredPeer(peerId = peerId, transport = TransportType.LAN, rssi = -30)
        )

        val peer = neighborTable.getPeer(peerId)
        assertThat(peer).isNotNull()
        assertThat(peer!!.transports).containsExactly(TransportType.BLE, TransportType.LAN)
    }

    @Test
    fun `best transport should be highest priority`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        neighborTable.updatePeer(
            DiscoveredPeer(peerId = peerId, transport = TransportType.LAN)
        )
        neighborTable.updatePeer(
            DiscoveredPeer(peerId = peerId, transport = TransportType.BLE)
        )

        val peer = neighborTable.getPeer(peerId)
        assertThat(peer!!.bestTransport).isEqualTo(TransportType.BLE) // BLE has lowest ordinal
    }

    @Test
    fun `score link returns positive value`() {
        val entry = NeighborEntry(
            peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            rssi = -50,
            batteryLevel = 80,
            latencyMs = 30,
            packetLossRate = 0.02f,
            reliability = 0.9f,
            firstSeen = System.currentTimeMillis() - 120_000,
            lastSeen = System.currentTimeMillis(),
            transports = setOf(TransportType.BLE)
        )

        val score = router.scoreLink(entry)
        assertThat(score).isGreaterThan(0f)
        assertThat(score).isLessThan(1f)
    }

    @Test
    fun `stronger signal scores higher`() {
        val strong = NeighborEntry(
            peerId = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0),
            rssi = -30, transports = setOf(TransportType.BLE),
            firstSeen = System.currentTimeMillis() - 60_000,
            lastSeen = System.currentTimeMillis()
        )
        val weak = NeighborEntry(
            peerId = byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0),
            rssi = -90, transports = setOf(TransportType.BLE),
            firstSeen = System.currentTimeMillis() - 60_000,
            lastSeen = System.currentTimeMillis()
        )

        assertThat(router.scoreLink(strong)).isGreaterThan(router.scoreLink(weak))
    }

    @Test
    fun `reputation updates work`() {
        val peerId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        neighborTable.updatePeer(
            DiscoveredPeer(peerId = peerId, transport = TransportType.BLE)
        )

        val initialRep = neighborTable.getPeer(peerId)!!.reliability

        neighborTable.reportDeliverySuccess(peerId)
        val afterSuccess = neighborTable.getPeer(peerId)!!.reliability
        assertThat(afterSuccess).isGreaterThan(initialRep)

        neighborTable.reportDeliveryFailure(peerId)
        val afterFailure = neighborTable.getPeer(peerId)!!.reliability
        assertThat(afterFailure).isLessThan(afterSuccess)
    }
}
