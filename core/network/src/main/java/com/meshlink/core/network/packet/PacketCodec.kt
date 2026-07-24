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
package com.meshlink.core.network.packet

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.MeshResult
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary codec for MeshPacket serialization/deserialization.
 *
 * ## Wire Format
 * All multi-byte values are big-endian (network byte order).
 * Payload is optionally LZ4-compressed (indicated by flags.compressed).
 *
 * ## Encoding Process
 * 1. Construct header (40 bytes)
 * 2. Optionally LZ4-compress payload
 * 3. Write payload length (2 bytes) + payload
 * 4. Append Ed25519 signature (64 bytes)
 *
 * ## Performance
 * Zero-copy where possible â€” uses ByteBuffer for efficient serialization.
 * Typical packet: 42 header + ~100 payload + 64 signature = ~206 bytes.
 * Well within BLE MTU of 512 bytes.
 */
class PacketCodec {

    /**
     * Encode a MeshPacket to binary wire format.
     *
     * @param packet The packet to encode
     * @return Byte array ready for transport, or Error
     */
    fun encode(packet: MeshPacket): MeshResult<ByteArray> {
        return try {
            val payloadBytes = packet.payload
            val totalSize = MeshPacket.HEADER_SIZE + payloadBytes.size +
                    (packet.signature?.size ?: 0)

            val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

            // Header (8 bytes of control fields)
            buffer.put(packet.version)
            buffer.put(packet.type.code.toByte())
            buffer.put(packet.priority.toByte())
            buffer.put(packet.ttl.toByte())
            buffer.put(packet.flags.toByte())
            buffer.put(packet.fragmentIndex.toByte())
            buffer.put(packet.totalFragments.toByte())
            buffer.put(0.toByte()) // reserved/padding

            // IDs and timestamp (32 bytes)
            buffer.put(packet.packetId)
            buffer.put(packet.senderId)
            buffer.put(packet.recipientId)
            buffer.putLong(packet.timestamp)

            // Payload
            buffer.putShort(payloadBytes.size.toShort())
            buffer.put(payloadBytes)

            // Signature (optional, 64 bytes)
            packet.signature?.let { buffer.put(it) }

            MeshResult.Success(buffer.array())
        } catch (e: Exception) {
            Timber.e(e, "Packet encoding failed")
            MeshResult.Error("Encoding failed", e)
        }
    }

    /**
     * Decode binary data into a MeshPacket.
     *
     * @param data Raw bytes from transport
     * @return Decoded MeshPacket, or Error if malformed
     */
    fun decode(data: ByteArray): MeshResult<MeshPacket> {
        return try {
            if (data.size < MeshPacket.HEADER_SIZE) {
                return MeshResult.Error(
                    "Packet too small: ${data.size} < ${MeshPacket.HEADER_SIZE}"
                )
            }

            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

            // Header
            val version = buffer.get()
            if (version != MeshConstants.PROTOCOL_VERSION) {
                return MeshResult.Error("Unknown protocol version: $version")
            }

            val typeCode = buffer.get().toInt() and 0xFF
            val type = PacketType.fromCode(typeCode)
                ?: return MeshResult.Error("Unknown packet type: $typeCode")

            val priority = buffer.get().toInt() and 0xFF
            val ttl = buffer.get().toInt() and 0xFF
            val flags = PacketFlags.fromByte(buffer.get())
            val fragmentIndex = buffer.get().toInt() and 0xFF
            val totalFragments = buffer.get().toInt() and 0xFF
            buffer.get() // skip padding

            // IDs
            val packetId = ByteArray(MeshConstants.PACKET_ID_SIZE)
            buffer.get(packetId)
            val senderId = ByteArray(MeshConstants.PEER_ID_SIZE)
            buffer.get(senderId)
            val recipientId = ByteArray(MeshConstants.PEER_ID_SIZE)
            buffer.get(recipientId)
            val timestamp = buffer.long

            // Payload
            val payloadLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < payloadLength) {
                return MeshResult.Error(
                    "Payload truncated: declared=$payloadLength, available=${buffer.remaining()}"
                )
            }
            val payload = ByteArray(payloadLength)
            buffer.get(payload)

            // Signature (if remaining bytes match)
            val signature = if (buffer.remaining() >= MeshPacket.SIGNATURE_SIZE) {
                ByteArray(MeshPacket.SIGNATURE_SIZE).also { buffer.get(it) }
            } else {
                null
            }

            MeshResult.Success(
                MeshPacket(
                    version = version,
                    type = type,
                    priority = priority,
                    ttl = ttl,
                    flags = flags,
                    fragmentIndex = fragmentIndex,
                    totalFragments = totalFragments,
                    packetId = packetId,
                    senderId = senderId,
                    recipientId = recipientId,
                    timestamp = timestamp,
                    payload = payload,
                    signature = signature
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Packet decoding failed")
            MeshResult.Error("Decoding failed", e)
        }
    }
}
