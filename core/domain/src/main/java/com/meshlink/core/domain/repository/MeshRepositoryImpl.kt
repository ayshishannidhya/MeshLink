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
package com.meshlink.core.domain.repository

import com.meshlink.core.mesh.MeshEngine
import com.meshlink.core.mesh.MeshStats
import com.meshlink.core.mesh.routing.NeighborEntry
import com.meshlink.core.network.transport.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshRepositoryImpl @Inject constructor(
    private val meshEngine: MeshEngine
) : MeshRepository {
    override val activePeers: Flow<List<NeighborEntry>>
        get() = meshEngine.neighbors.activePeersFlow
    override val meshStats: StateFlow<MeshStats>
        get() = meshEngine.meshStats
    override val isMeshActive: StateFlow<Boolean>
        get() = meshEngine.isActive
    override suspend fun setTransportEnabled(type: TransportType, enabled: Boolean) {
        meshEngine.setTransportEnabled(type, enabled)
    }
}
