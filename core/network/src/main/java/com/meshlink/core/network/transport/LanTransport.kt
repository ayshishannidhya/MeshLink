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
package com.meshlink.core.network.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.toShortHex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * LAN transport using mDNS/NSD discovery and TCP sockets.
 *
 * ## Use Case
 * When devices share the same Wi-Fi network (home, office, campus),
 * LAN transport provides:
 * - Zero-config discovery via mDNS (Bonjour-style)
 * - Full network bandwidth (~100+ Mbps)
 * - No pairing required
 * - Works alongside BLE and Wi-Fi Direct
 *
 * ## Architecture
 * ```
 * NSD Service Registration    â† Advertises _meshlink._tcp
 * NSD Service Discovery       â†’ Finds other MeshLink instances
 * TCP Server (port 8766)      â† Accepts incoming connections
 * TCP Client                  â†’ Connects to discovered peers
 * ```
 *
 * ## Limitations
 * - Requires shared Wi-Fi network
 * - Doesn't work without any network (BLE/Wi-Fi Direct cover that)
 * - Some enterprise networks block mDNS or peer-to-peer TCP
 */
class LanTransport(
    private val context: Context,
    private val localPeerId: ByteArray
) : MeshTransport {

    override val transportType = TransportType.LAN
    override var isActive: Boolean = false
        private set

    private val _incomingPackets = MutableSharedFlow<TransportPacket>(extraBufferCapacity = 32)
    override val incomingPackets: SharedFlow<TransportPacket> = _incomingPackets.asSharedFlow()

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 16)
    override val discoveredPeers: Flow<DiscoveredPeer> = _discoveredPeers.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var nsdManager: NsdManager? = null
    private var serverSocket: ServerSocketChannel? = null
    private val activeConnections = mutableMapOf<String, SocketChannel>()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        const val SERVICE_TYPE = "_meshlink._tcp"
        const val SERVICE_NAME = "MeshLink"
        const val PORT = 8766
    }

    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun start() {
        if (isActive) return
        isActive = true
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

        startTcpServer()
        registerService()
        discoverServices()

        Timber.i("LAN Transport started")
    }

    override suspend fun stop() {
        isActive = false

        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) { }

        serverSocket?.close()
        activeConnections.values.forEach { runCatching { it.close() } }
        activeConnections.clear()
        scope.cancel()

        Timber.i("LAN Transport stopped")
    }

    override suspend fun sendPacket(peerId: ByteArray?, data: ByteArray): Boolean {
        if (peerId == null) {
            return broadcastToAll(data)
        }
        val key = peerId.toShortHex()
        val conn = activeConnections[key] ?: return false
        return writeFramed(conn, data)
    }

    override fun getHealthMetrics(): TransportHealth {
        return TransportHealth(
            isConnected = activeConnections.isNotEmpty(),
            peerCount = activeConnections.size
        )
    }

    // â”€â”€ mDNS Service Registration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun registerService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME-${localPeerId.toShortHex()}"
            serviceType = SERVICE_TYPE
            port = PORT
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Timber.d("NSD service registered: ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Timber.e("NSD registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Timber.d("NSD service unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Timber.e("NSD unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    // â”€â”€ mDNS Service Discovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun discoverServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("NSD discovery started for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains(SERVICE_TYPE.replace(".", ""))) {
                    // Don't resolve our own service
                    if (serviceInfo.serviceName.contains(localPeerId.toShortHex())) return

                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            Timber.w("NSD resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host
                            val port = info.port

                            scope.launch {
                                val peerId = extractPeerIdFromName(info.serviceName)
                                _discoveredPeers.emit(
                                    DiscoveredPeer(
                                        peerId = peerId,
                                        transport = TransportType.LAN,
                                        displayName = info.serviceName
                                    )
                                )

                                // Connect if not already connected
                                val key = peerId.toShortHex()
                                if (!activeConnections.containsKey(key)) {
                                    connectToPeer(host, port, peerId)
                                }
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("NSD service lost: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("NSD discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NSD start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NSD stop discovery failed: $errorCode")
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    // â”€â”€ TCP Server â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startTcpServer() {
        scope.launch {
            try {
                serverSocket = ServerSocketChannel.open().apply {
                    socket().reuseAddress = true
                    socket().bind(InetSocketAddress(PORT))
                }
                Timber.d("LAN TCP server listening on port $PORT")

                while (isActive) {
                    val client = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    } ?: continue

                    scope.launch { handleIncomingConnection(client) }
                }
            } catch (e: Exception) {
                Timber.e(e, "LAN TCP server error")
            }
        }
    }

    private suspend fun handleIncomingConnection(channel: SocketChannel) {
        try {
            channel.configureBlocking(true)
            while (isActive && channel.isConnected) {
                val data = readFramed(channel) ?: break
                _incomingPackets.emit(
                    TransportPacket(
                        data = data,
                        fromPeerId = null,
                        transport = TransportType.LAN
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "LAN TCP read error")
        } finally {
            channel.close()
        }
    }

    private suspend fun connectToPeer(host: InetAddress, port: Int, peerId: ByteArray) {
        try {
            val channel = withContext(Dispatchers.IO) {
                SocketChannel.open(InetSocketAddress(host, port))
            }
            val key = peerId.toShortHex()
            activeConnections[key] = channel
            Timber.d("LAN connected to $host:$port")

            scope.launch {
                try {
                    while (isActive && channel.isConnected) {
                        val data = readFramed(channel) ?: break
                        _incomingPackets.emit(
                            TransportPacket(data = data, fromPeerId = peerId, transport = TransportType.LAN)
                        )
                    }
                } finally {
                    activeConnections.remove(key)
                    channel.close()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "LAN connect failed to $host:$port")
        }
    }

    // â”€â”€ Framing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun writeFramed(channel: SocketChannel, data: ByteArray): Boolean {
        return try {
            val header = ByteBuffer.allocate(4)
            header.putInt(data.size)
            header.flip()
            channel.write(header)
            channel.write(ByteBuffer.wrap(data))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readFramed(channel: SocketChannel): ByteArray? {
        val header = ByteBuffer.allocate(4)
        while (header.hasRemaining()) {
            if (channel.read(header) == -1) return null
        }
        header.flip()
        val length = header.int
        if (length <= 0 || length > 1_048_576) return null

        val data = ByteBuffer.allocate(length)
        while (data.hasRemaining()) {
            if (channel.read(data) == -1) return null
        }
        data.flip()
        return data.array()
    }

    private suspend fun broadcastToAll(data: ByteArray): Boolean {
        var success = false
        activeConnections.values.toList().forEach {
            if (writeFramed(it, data)) success = true
        }
        return success
    }

    private fun extractPeerIdFromName(serviceName: String): ByteArray {
        val hexPart = serviceName.substringAfterLast("-", "")
        return if (hexPart.length == 8) {
            hexPart.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } else {
            MeshConstants.generateId()
        }
    }
}
