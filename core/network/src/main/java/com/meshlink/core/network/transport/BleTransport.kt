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
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.toShortHex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID

/**
 * Bluetooth Low Energy mesh transport implementation.
 *
 * ## Architecture
 * Runs simultaneously as:
 * - **GATT Server**: Accepts incoming connections, exposes write/notify characteristics
 * - **GATT Client**: Connects to discovered peripherals, writes data
 * - **Advertiser**: Broadcasts presence with peer ID in manufacturer data
 * - **Scanner**: Discovers nearby MeshLink devices
 *
 * ## BLE Protocol Flow
 * ```
 * Discovery:  Scanner finds advertiser via service UUID
 * Connect:    Client connects to GATT server
 * MTU:        Negotiate 517-byte MTU
 * Exchange:   Write to write-characteristic â†’ Server notifies on read-characteristic
 * Relay:      Forward packets to other connected devices
 * ```
 *
 * ## Duty Cycling
 * - Normal mode: Scan 4s, pause 8s (33% duty cycle)
 * - Low power: Scan 2s, pause 15s (12% duty cycle)
 * - Adaptive: Increase scan rate when isolated, decrease when heavily connected
 *
 * ## Limits
 * - Max 7 simultaneous GATT connections (Android limit)
 * - MTU: 512 bytes (negotiated from 517 request)
 * - Throughput: ~20-25 KB/s per connection
 *
 * @param context Android application context
 * @param localPeerId Our 8-byte peer ID for identification
 */
@SuppressLint("MissingPermission")
class BleTransport(
    private val context: Context,
    private val localPeerId: ByteArray
) : MeshTransport {

    override val transportType = TransportType.BLE
    override var isActive: Boolean = false
        private set

    private val _incomingPackets = MutableSharedFlow<TransportPacket>(extraBufferCapacity = 32)
    override val incomingPackets: SharedFlow<TransportPacket> = _incomingPackets.asSharedFlow()

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 16)
    override val discoveredPeers: Flow<DiscoveredPeer> = _discoveredPeers.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceUuid = UUID.fromString(MeshConstants.BLE_SERVICE_UUID)
    private val writeCharUuid = UUID.fromString(MeshConstants.BLE_WRITE_CHAR_UUID)
    private val readCharUuid = UUID.fromString(MeshConstants.BLE_READ_CHAR_UUID)
    private val notifyCharUuid = UUID.fromString(MeshConstants.BLE_NOTIFY_CHAR_UUID)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    private val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

    // Connected GATT clients keyed by device address
    private val connectedDevices = mutableMapOf<String, BluetoothGatt>()
    private val deviceToPeerId = mutableMapOf<String, ByteArray>()

    private var gattServer: BluetoothGattServer? = null
    private var scanJob: Job? = null

    // â”€â”€ Transport Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun start() {
        if (isActive) return
        isActive = true

        startGattServer()
        startAdvertising()
        startScanning()

        Timber.i("BLE Transport started")
    }

    override suspend fun stop() {
        isActive = false
        scanJob?.cancel()

        bleScanner?.stopScan(scanCallback)
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        connectedDevices.values.forEach { it.close() }
        connectedDevices.clear()
        gattServer?.close()
        gattServer = null

        scope.cancel()
        Timber.i("BLE Transport stopped")
    }

    override suspend fun sendPacket(peerId: ByteArray?, data: ByteArray): Boolean {
        if (peerId == null) {
            // Broadcast to all connected devices
            return broadcastToAll(data)
        }

        val peerHex = peerId.toShortHex()
        val deviceAddress = deviceToPeerId.entries
            .find { it.value.toShortHex() == peerHex }?.key
            ?: return false

        val gatt = connectedDevices[deviceAddress] ?: return false
        return writeToDevice(gatt, data)
    }

    override fun getHealthMetrics(): TransportHealth {
        return TransportHealth(
            isConnected = connectedDevices.isNotEmpty(),
            peerCount = connectedDevices.size
        )
    }

    // â”€â”€ GATT Server â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startGattServer() {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(
            serviceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Write characteristic â€” clients write packets here
        val writeChar = BluetoothGattCharacteristic(
            writeCharUuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Notify characteristic â€” server pushes packets to clients
        val notifyChar = BluetoothGattCharacteristic(
            notifyCharUuid,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Add CCCD for notifications
        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
        )
        notifyChar.addDescriptor(cccd)

        service.addCharacteristic(writeChar)
        service.addCharacteristic(notifyChar)

        gattServer?.addService(service)
        Timber.d("GATT Server started with service: $serviceUuid")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("GATT Server: device connected: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device.address)?.close()
                    deviceToPeerId.remove(device.address)
                    Timber.d("GATT Server: device disconnected: ${device.address}")
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray
        ) {
            if (characteristic.uuid == writeCharUuid) {
                // Received a mesh packet
                scope.launch {
                    val peerId = deviceToPeerId[device.address]
                    _incomingPackets.emit(
                        TransportPacket(
                            data = value,
                            fromPeerId = peerId,
                            transport = TransportType.BLE
                        )
                    )
                }
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    // â”€â”€ BLE Advertising â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)  // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(serviceUuid))
            .addManufacturerData(0xFF01, localPeerId)  // Embed peer ID
            .build()

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.d("BLE Advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.e("BLE Advertising failed: $errorCode")
        }
    }

    // â”€â”€ BLE Scanning with Duty Cycling â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startScanning() {
        scanJob = scope.launch {
            while (isActive) {
                performScan()
                delay(MeshConstants.SCAN_DURATION_NORMAL_MS)
                bleScanner?.stopScan(scanCallback)
                delay(MeshConstants.SCAN_PAUSE_NORMAL_MS)
            }
        }
    }

    private fun performScan() {
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bleScanner?.startScan(filters, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val mfgData = result.scanRecord?.getManufacturerSpecificData(0xFF01)

            scope.launch {
                _discoveredPeers.emit(
                    DiscoveredPeer(
                        peerId = mfgData ?: MeshConstants.generateId(),
                        transport = TransportType.BLE,
                        rssi = rssi,
                        displayName = device.name
                    )
                )
            }

            // Auto-connect if not already connected and under limit
            if (!connectedDevices.containsKey(device.address) &&
                connectedDevices.size < MeshConstants.BLE_MAX_CONNECTIONS
            ) {
                connectToDevice(device, mfgData)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("BLE Scan failed: $errorCode")
        }
    }

    // â”€â”€ GATT Client â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun connectToDevice(device: BluetoothDevice, peerId: ByteArray?) {
        val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevices[device.address] = gatt
                    peerId?.let { deviceToPeerId[device.address] = it }
                    gatt.requestMtu(MeshConstants.BLE_MTU_REQUEST)
                    Timber.d("Connected to ${device.address}")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedDevices.remove(device.address)
                    deviceToPeerId.remove(device.address)
                    gatt.close()
                    Timber.d("Disconnected from ${device.address}")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("MTU negotiated: $mtu for ${device.address}")
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(serviceUuid)
                    val notifyChar = service?.getCharacteristic(notifyCharUuid)
                    if (notifyChar != null) {
                        gatt.setCharacteristicNotification(notifyChar, true)
                        Timber.d("Subscribed to notifications from ${device.address}")
                    }
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                // Received data via notification
                val value = characteristic.value ?: return
                scope.launch {
                    _incomingPackets.emit(
                        TransportPacket(
                            data = value,
                            fromPeerId = deviceToPeerId[device.address],
                            transport = TransportType.BLE
                        )
                    )
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    // â”€â”€ Write Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun writeToDevice(gatt: BluetoothGatt, data: ByteArray): Boolean {
        val service = gatt.getService(serviceUuid) ?: return false
        val writeChar = service.getCharacteristic(writeCharUuid) ?: return false

        return try {
            @Suppress("DEPRECATION")
            writeChar.value = data
            writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(writeChar)
        } catch (e: Exception) {
            Timber.e(e, "BLE write failed")
            false
        }
    }

    private suspend fun broadcastToAll(data: ByteArray): Boolean {
        var anySuccess = false
        connectedDevices.values.forEach { gatt ->
            if (writeToDevice(gatt, data)) {
                anySuccess = true
            }
        }
        return anySuccess
    }
}
