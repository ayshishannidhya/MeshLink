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
import com.meshlink.core.mesh.routing.MeshRouter
import com.meshlink.core.mesh.routing.NeighborTable
import com.meshlink.core.mesh.flood.FloodController
import com.meshlink.core.mesh.store.StoreForwardManager
import com.meshlink.core.network.packet.MeshPacket
import com.meshlink.core.network.packet.PacketCodec
import com.meshlink.core.network.packet.PacketType
import com.meshlink.core.network.transport.MeshTransport
import com.meshlink.core.network.transport.TransportPacket
import com.meshlink.core.network.transport.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central mesh networking engine.
 *
 * Orchestrates all mesh operations:
 * - Transport management (BLE, Wi-Fi Direct, LAN)
 * - Packet routing (weighted shortest path + controlled flood)
 * - Store-and-forward for offline delivery (DTN)
 * - Peer discovery and neighbor table maintenance
 * - Anti-spam and rate limiting
 *
 * ## Architecture
 * ```
 * Transports â†’ [inbound] â†’ FloodController â†’ MeshRouter â†’ Application
 *                                          â†“
 *                              StoreForwardManager (for offline peers)
 * ```
 *
 * ## Thread Safety
 * All mutable state is accessed on a single-threaded dispatcher.
 */
@Singleton
class MeshEngine @Inject constructor(
    private val transports: Set<@JvmSuppressWildcards MeshTransport>,
    private val router: MeshRouter,
    private val floodController: FloodController,
    private val storeForwardManager: StoreForwardManager,
    private val neighborTable: NeighborTable,
    private val packetCodec: PacketCodec,
    private val rateLimiter: RateLimiter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val meshDispatcher = Dispatchers.Default.limitedParallelism(1)

    private val _incomingMessages = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    /** Flow of fully-processed incoming messages for the application layer. */
    val incomingMessages: SharedFlow<MeshPacket> = _incomingMessages.asSharedFlow()

    val neighbors: NeighborTable get() = neighborTable

    private val _meshStats = MutableStateFlow(MeshStats())
    /** Observable mesh statistics. */
    val meshStats: StateFlow<MeshStats> = _meshStats.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    /** Whether the mesh engine is currently running. */
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var isRunning = false
    private var localPeerId: ByteArray = ByteArray(0)

    /**
     * Start the mesh engine with all available transports.
     */
    suspend fun start(peerId: ByteArray = MeshConstants.generateId()) {
        if (isRunning) return
        isRunning = true
        _isActive.value = true
        localPeerId = peerId

        Timber.i("MeshEngine starting with peer ID: ${peerId.toShortHex()}")

        // Start all transports
        transports.forEach { transport ->
            scope.launch {
                try {
                    transport.start()
                    Timber.i("Transport ${transport.transportType} started")
                    collectTransportPackets(transport)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start transport ${transport.transportType}")
                }
            }

            // Collect discovered peers
            scope.launch {
                transport.discoveredPeers.collect { peer ->
                    withContext(meshDispatcher) {
                        neighborTable.updatePeer(peer)
                    }
                }
            }
        }

        // Periodic maintenance
        scope.launch { runMaintenanceLoop() }

        // Process store-and-forward queue
        scope.launch { storeForwardManager.startRetryLoop(::sendPacketToTransport) }
    }

    suspend fun setTransportEnabled(type: TransportType, enabled: Boolean) {
        val transport = transports.find { it.transportType == type } ?: return
        if (enabled && !transport.isActive) {
            transport.start()
            Timber.i("Transport ${type} enabled")
        } else if (!enabled && transport.isActive) {
            transport.stop()
            Timber.i("Transport ${type} disabled")
        }
    }

    /**
     * Stop the mesh engine and all transports.
     */
    suspend fun stop() {
        isRunning = false
        _isActive.value = false
        scope.coroutineContext.cancelChildren()
        transports.forEach { it.stop() }
        Timber.i("MeshEngine stopped")
    }

    /**
     * Send a packet through the mesh.
     * Automatically chooses routing strategy (direct, source-routed, or flood).
     */
    suspend fun sendPacket(packet: MeshPacket) {
        withContext(meshDispatcher) {
            // Mark as seen to prevent self-relay
            floodController.markSeen(packet)

            // Try direct route first
            val routed = router.findRoute(packet.recipientId)
            if (routed != null && !packet.isBroadcast) {
                val success = sendPacketToTransport(packet, routed.nextHopPeerId, routed.transport)
                if (success) {
                    updateStats { it.copy(packetsSent = it.packetsSent + 1) }
                    return@withContext
                }
            }

            // Fallback: flood broadcast
            if (packet.isBroadcast) {
                floodBroadcast(packet)
            } else {
                // Unicast but no route: try flood + store-and-forward
                floodBroadcast(packet)
                storeForwardManager.enqueue(packet)
            }

            updateStats { it.copy(packetsSent = it.packetsSent + 1) }
        }
    }

    // â”€â”€ Internal Packet Processing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private suspend fun collectTransportPackets(transport: MeshTransport) {
        transport.incomingPackets.collect { transportPacket ->
            withContext(meshDispatcher) {
                processIncomingPacket(transportPacket)
            }
        }
    }

    private suspend fun processIncomingPacket(transportPacket: TransportPacket) {
        val decodeResult = packetCodec.decode(transportPacket.data)
        val packet = decodeResult.getOrNull() ?: run {
            Timber.w("Failed to decode packet from ${transportPacket.transport}")
            return
        }

        // Rate limiting â€” prevent flood attacks
        if (!rateLimiter.allowPacket(packet.senderId)) {
            Timber.w("Rate limited peer: ${packet.senderId.toShortHex()}")
            updateStats { it.copy(packetsDropped = it.packetsDropped + 1) }
            return
        }

        // Dedup â€” drop if already seen
        if (!floodController.shouldProcess(packet)) {
            return
        }

        updateStats { it.copy(packetsReceived = it.packetsReceived + 1) }

        // Update neighbor table from received packet
        neighborTable.recordContact(packet.senderId, transportPacket.transport, transportPacket.rssi)

        // Is this packet for us?
        if (packet.isBroadcast || packet.recipientId.contentEquals(localPeerId)) {
            deliverToApplication(packet)
        }

        // Check store-and-forward: do we have queued packets for the sender?
        storeForwardManager.checkAndDeliver(packet.senderId, ::sendPacketToTransport)

        // Relay if TTL allows and not for us specifically
        if (packet.ttl > 0 && !packet.recipientId.contentEquals(localPeerId)) {
            val relayPacket = packet.forRelay() ?: return
            floodController.scheduleRelay(relayPacket, transportPacket.fromPeerId) {
                scope.launch { floodBroadcast(it, exclude = transportPacket.fromPeerId) }
            }
        }
    }

    private suspend fun deliverToApplication(packet: MeshPacket) {
        _incomingMessages.emit(packet)
    }

    private suspend fun floodBroadcast(
        packet: MeshPacket,
        exclude: ByteArray? = null
    ) {
        val targets = floodController.selectFanoutTargets(
            neighborTable.getActivePeers(),
            packet,
            exclude
        )

        val encodedResult = packetCodec.encode(packet)
        val encoded = encodedResult.getOrNull() ?: return

        targets.forEach { (peerId, transport) ->
            scope.launch {
                sendPacketToTransport(encoded, peerId, transport)
            }
        }
    }

    private suspend fun sendPacketToTransport(
        packet: MeshPacket,
        peerId: ByteArray,
        transport: com.meshlink.core.network.transport.TransportType
    ): Boolean {
        val encodedResult = packetCodec.encode(packet)
        val encoded = encodedResult.getOrNull() ?: return false
        return sendPacketToTransport(encoded, peerId, transport)
    }

    private suspend fun sendPacketToTransport(
        data: ByteArray,
        peerId: ByteArray,
        transport: com.meshlink.core.network.transport.TransportType
    ): Boolean {
        val targetTransport = transports.find { it.transportType == transport } ?: return false
        return targetTransport.sendPacket(peerId, data)
    }

    // â”€â”€ Maintenance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private suspend fun runMaintenanceLoop() {
        while (isRunning) {
            delay(30_000) // Every 30 seconds
            withContext(meshDispatcher) {
                neighborTable.pruneStale()
                storeForwardManager.pruneExpired()
                floodController.pruneDedup()
                updateStats { stats ->
                    stats.copy(
                        activePeers = neighborTable.getActivePeers().size,
                        pendingMessages = storeForwardManager.queueSize()
                    )
                }
            }
        }
    }

    private fun updateStats(update: (MeshStats) -> MeshStats) {
        _meshStats.update(update)
    }
}

/**
 * Observable mesh network statistics.
 */
data class MeshStats(
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsRelayed: Long = 0,
    val packetsDropped: Long = 0,
    val activePeers: Int = 0,
    val pendingMessages: Int = 0,
    val averageHopCount: Float = 0f,
    val uptimeMs: Long = 0
)
