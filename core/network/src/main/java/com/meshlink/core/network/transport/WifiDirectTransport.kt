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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.toShortHex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * Wi-Fi Direct (P2P) transport for high-bandwidth mesh communication.
 *
 * ## Why Wi-Fi Direct?
 * - ~250 Mbps bandwidth vs BLE's ~25 KB/s
 * - Range: ~200m vs BLE's ~30-50m
 * - Ideal for file transfers and voice notes
 * - No router needed â€” devices connect directly
 *
 * ## Architecture
 * ```
 * WifiP2pManager
 *   â”œâ”€â”€ discoverPeers()        â†’ Find nearby devices
 *   â”œâ”€â”€ connect()              â†’ Form P2P group
 *   â”œâ”€â”€ Group Owner (Server)   â†’ Accepts TCP connections on PORT
 *   â””â”€â”€ Client                 â†’ Connects to Group Owner's IP
 * ```
 *
 * ## Data Transfer
 * Uses TCP sockets (not UDP) for reliable delivery:
 * - Length-prefixed framing: [4-byte length][packet data]
 * - Connection pooling: reuse TCP connections
 * - Automatic reconnection on disconnect
 *
 * ## Limitations
 * - Only 1 P2P group at a time (Android limitation)
 * - Group formation takes 2-5 seconds
 * - Higher power consumption than BLE
 * - Not all devices support concurrent Wi-Fi + P2P
 */
@SuppressLint("MissingPermission")
class WifiDirectTransport(
    private val context: Context,
    private val localPeerId: ByteArray
) : MeshTransport {

    override val transportType = TransportType.WIFI_DIRECT
    override var isActive: Boolean = false
        private set

    private val _incomingPackets = MutableSharedFlow<TransportPacket>(extraBufferCapacity = 32)
    override val incomingPackets: SharedFlow<TransportPacket> = _incomingPackets.asSharedFlow()

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 16)
    override val discoveredPeers: Flow<DiscoveredPeer> = _discoveredPeers.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: Channel? = null
    private var receiver: BroadcastReceiver? = null

    // TCP server for incoming connections
    private var serverSocket: ServerSocketChannel? = null
    private val activeConnections = mutableMapOf<String, SocketChannel>()

    private var isGroupOwner = false
    private var groupOwnerAddress: String? = null

    companion object {
        const val PORT = 8765
        const val HEADER_SIZE = 4  // 4-byte length prefix
        const val MAX_PACKET_SIZE = 1_048_576  // 1 MiB
    }

    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun start() {
        if (isActive) return
        isActive = true

        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)

        registerReceiver()
        startTcpServer()
        startDiscovery()

        Timber.i("Wi-Fi Direct Transport started")
    }

    override suspend fun stop() {
        isActive = false

        wifiP2pManager?.removeGroup(channel, null)
        wifiP2pManager?.stopPeerDiscovery(channel, null)

        serverSocket?.close()
        activeConnections.values.forEach { runCatching { it.close() } }
        activeConnections.clear()

        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) { }

        scope.cancel()
        Timber.i("Wi-Fi Direct Transport stopped")
    }

    override suspend fun sendPacket(peerId: ByteArray?, data: ByteArray): Boolean {
        if (peerId == null) {
            // Broadcast to all active connections
            return broadcastToAll(data)
        }

        val peerHex = peerId.toShortHex()
        val connection = activeConnections[peerHex] ?: return false
        return writeFramed(connection, data)
    }

    override fun getHealthMetrics(): TransportHealth {
        return TransportHealth(
            isConnected = activeConnections.isNotEmpty(),
            peerCount = activeConnections.size
        )
    }

    // â”€â”€ Wi-Fi P2P Discovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startDiscovery() {
        scope.launch {
            while (isActive) {
                wifiP2pManager?.discoverPeers(channel, object : ActionListener {
                    override fun onSuccess() {
                        Timber.d("Wi-Fi Direct discovery started")
                    }
                    override fun onFailure(reason: Int) {
                        Timber.w("Wi-Fi Direct discovery failed: $reason")
                    }
                })
                delay(30_000) // Re-discover every 30s
            }
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        wifiP2pManager?.requestPeers(channel) { peers ->
                            handlePeersDiscovered(peers)
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            handleConnectionInfo(info)
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, filter)
    }

    private fun handlePeersDiscovered(peerList: WifiP2pDeviceList) {
        peerList.deviceList.forEach { device ->
            scope.launch {
                _discoveredPeers.emit(
                    DiscoveredPeer(
                        peerId = device.deviceAddress.toByteArray(),
                        transport = TransportType.WIFI_DIRECT,
                        displayName = device.deviceName
                    )
                )
            }

            // Auto-connect to first available peer
            if (activeConnections.isEmpty()) {
                connectToPeer(device)
            }
        }
    }

    private fun connectToPeer(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiP2pManager?.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                Timber.d("Wi-Fi Direct connection initiated to ${device.deviceName}")
            }
            override fun onFailure(reason: Int) {
                Timber.w("Wi-Fi Direct connect failed: $reason")
            }
        })
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) return

        isGroupOwner = info.isGroupOwner
        groupOwnerAddress = info.groupOwnerAddress?.hostAddress

        if (!isGroupOwner && groupOwnerAddress != null) {
            // Client: connect to group owner's TCP server
            scope.launch {
                connectToGroupOwner(groupOwnerAddress!!)
            }
        }
        // If group owner, the TCP server is already running
    }

    // â”€â”€ TCP Socket Layer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startTcpServer() {
        scope.launch {
            try {
                serverSocket = ServerSocketChannel.open().apply {
                    socket().reuseAddress = true
                    socket().bind(InetSocketAddress(PORT))
                    configureBlocking(false)
                }

                Timber.d("TCP server started on port $PORT")

                while (isActive) {
                    val client = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    if (client != null) {
                        handleIncomingConnection(client)
                    } else {
                        delay(100) // Non-blocking poll interval
                    }
                }
            } catch (e: IOException) {
                Timber.e(e, "TCP server error")
            }
        }
    }

    private fun handleIncomingConnection(channel: SocketChannel) {
        val address = channel.remoteAddress.toString()
        Timber.d("TCP connection from $address")

        scope.launch {
            try {
                channel.configureBlocking(true)
                while (isActive && channel.isConnected) {
                    val data = readFramed(channel) ?: break
                    _incomingPackets.emit(
                        TransportPacket(
                            data = data,
                            fromPeerId = null,
                            transport = TransportType.WIFI_DIRECT
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "TCP read error from $address")
            } finally {
                channel.close()
            }
        }
    }

    private suspend fun connectToGroupOwner(address: String) {
        try {
            val socketChannel = withContext(Dispatchers.IO) {
                SocketChannel.open(InetSocketAddress(address, PORT))
            }
            val key = address
            activeConnections[key] = socketChannel

            Timber.d("Connected to group owner at $address")

            // Read loop
            scope.launch {
                try {
                    while (isActive && socketChannel.isConnected) {
                        val data = readFramed(socketChannel) ?: break
                        _incomingPackets.emit(
                            TransportPacket(
                                data = data,
                                fromPeerId = null,
                                transport = TransportType.WIFI_DIRECT
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "TCP read error")
                } finally {
                    activeConnections.remove(key)
                    socketChannel.close()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to group owner at $address")
        }
    }

    // â”€â”€ Length-Prefixed Framing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun writeFramed(channel: SocketChannel, data: ByteArray): Boolean {
        return try {
            val header = ByteBuffer.allocate(HEADER_SIZE)
            header.putInt(data.size)
            header.flip()
            channel.write(header)
            channel.write(ByteBuffer.wrap(data))
            true
        } catch (e: Exception) {
            Timber.e(e, "TCP write failed")
            false
        }
    }

    private fun readFramed(channel: SocketChannel): ByteArray? {
        val header = ByteBuffer.allocate(HEADER_SIZE)
        while (header.hasRemaining()) {
            if (channel.read(header) == -1) return null
        }
        header.flip()
        val length = header.int

        if (length <= 0 || length > MAX_PACKET_SIZE) {
            Timber.w("Invalid frame length: $length")
            return null
        }

        val data = ByteBuffer.allocate(length)
        while (data.hasRemaining()) {
            if (channel.read(data) == -1) return null
        }
        data.flip()
        return data.array()
    }

    private suspend fun broadcastToAll(data: ByteArray): Boolean {
        var anySuccess = false
        activeConnections.values.toList().forEach { connection ->
            if (writeFramed(connection, data)) anySuccess = true
        }
        return anySuccess
    }
}
