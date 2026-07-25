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
package com.meshlink.core.domain.usecase

import com.meshlink.core.domain.model.NetworkHealth
import com.meshlink.core.mesh.MeshStats
import com.meshlink.core.mesh.routing.NeighborEntry
import javax.inject.Inject

class CalculateNetworkHealthUseCase @Inject constructor() {
    operator fun invoke(peers: List<NeighborEntry>, stats: MeshStats): NetworkHealth {
        if (peers.isEmpty()) return NetworkHealth.NO_PEERS

        val avgRssi = peers.map { it.rssi }.average()
        val count = peers.size

        return when {
            avgRssi > -50 && count >= 3 -> NetworkHealth.EXCELLENT
            avgRssi > -65 && count >= 2 -> NetworkHealth.GOOD
            avgRssi > -80 || count >= 1 -> NetworkHealth.WEAK
            else -> NetworkHealth.CRITICAL
        }
    }
}
