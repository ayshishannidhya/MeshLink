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

import com.google.common.truth.Truth.assertThat
import com.meshlink.core.common.MeshConstants
import org.junit.Test

/**
 * Tests for PacketCodec round-trip encoding/decoding.
 * Verifies all header fields, payload, and signature survive serialization.
 */
class PacketCodecTest {

    private val codec = PacketCodec()

    @Test
    fun `encode and decode preserves all fields`() {
        val senderId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val recipientId = byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1)
        val packetId = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80)
        val payload = "Hello MeshLink!".toByteArray()

        val packet = MeshPacket(
            version = MeshConstants.PROTOCOL_VERSION,
            type = PacketType.MESSAGE,
            priority = MeshConstants.PRIORITY_MESSAGE,
            ttl = 5,
            flags = PacketFlags(compressed = true, encrypted = true),
            fragmentIndex = 2,
            totalFragments = 5,
            packetId = packetId,
            senderId = senderId,
            recipientId = recipientId,
            timestamp = 1234567890L,
            payload = payload
        )

        val encoded = codec.encode(packet)
        assertThat(encoded.isSuccess).isTrue()

        val decoded = codec.decode(encoded.getOrNull()!!)
        assertThat(decoded.isSuccess).isTrue()

        val result = decoded.getOrNull()!!
        assertThat(result.version).isEqualTo(MeshConstants.PROTOCOL_VERSION)
        assertThat(result.type).isEqualTo(PacketType.MESSAGE)
        assertThat(result.priority).isEqualTo(MeshConstants.PRIORITY_MESSAGE)
        assertThat(result.ttl).isEqualTo(5)
        assertThat(result.flags.compressed).isTrue()
        assertThat(result.flags.encrypted).isTrue()
        assertThat(result.flags.requiresAck).isFalse()
        assertThat(result.fragmentIndex).isEqualTo(2)
        assertThat(result.totalFragments).isEqualTo(5)
        assertThat(result.packetId).isEqualTo(packetId)
        assertThat(result.senderId).isEqualTo(senderId)
        assertThat(result.recipientId).isEqualTo(recipientId)
        assertThat(result.timestamp).isEqualTo(1234567890L)
        assertThat(result.payload).isEqualTo(payload)
    }

    @Test
    fun `broadcast packet has all-zero recipient`() {
        val packet = MeshPacket(
            type = PacketType.ANNOUNCE,
            senderId = MeshConstants.generateId()
        )
        assertThat(packet.isBroadcast).isTrue()
    }

    @Test
    fun `unicast packet is not broadcast`() {
        val packet = MeshPacket(
            type = PacketType.MESSAGE,
            senderId = MeshConstants.generateId(),
            recipientId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        )
        assertThat(packet.isBroadcast).isFalse()
    }

    @Test
    fun `empty payload round trip`() {
        val packet = MeshPacket(
            type = PacketType.ACK,
            senderId = MeshConstants.generateId(),
            payload = ByteArray(0)
        )

        val encoded = codec.encode(packet)
        val decoded = codec.decode(encoded.getOrNull()!!)
        assertThat(decoded.getOrNull()!!.payload).isEmpty()
    }

    @Test
    fun `maximum payload round trip`() {
        val largePayload = ByteArray(500) { it.toByte() }
        val packet = MeshPacket(
            type = PacketType.MESSAGE,
            senderId = MeshConstants.generateId(),
            payload = largePayload
        )

        val encoded = codec.encode(packet)
        val decoded = codec.decode(encoded.getOrNull()!!)
        assertThat(decoded.getOrNull()!!.payload).isEqualTo(largePayload)
    }

    @Test
    fun `decode rejects truncated data`() {
        val result = codec.decode(ByteArray(10))
        assertThat(result.isError).isTrue()
    }

    @Test
    fun `decode rejects unknown version`() {
        val data = ByteArray(42)
        data[0] = 0xFF.toByte()  // Invalid version
        val result = codec.decode(data)
        assertThat(result.isError).isTrue()
    }

    @Test
    fun `all packet types have unique codes`() {
        val codes = PacketType.entries.map { it.code }
        assertThat(codes).containsNoDuplicates()
    }

    @Test
    fun `packet flags round trip`() {
        val flags = PacketFlags(
            compressed = true,
            encrypted = false,
            requiresAck = true,
            isRelay = false,
            sourceRouted = true
        )
        val byte = flags.toByte()
        val restored = PacketFlags.fromByte(byte)
        assertThat(restored).isEqualTo(flags)
    }

    @Test
    fun `forRelay decrements TTL`() {
        val packet = MeshPacket(
            type = PacketType.MESSAGE,
            ttl = 5,
            senderId = MeshConstants.generateId()
        )
        val relayed = packet.forRelay()!!
        assertThat(relayed.ttl).isEqualTo(4)
    }

    @Test
    fun `forRelay returns null at TTL 0`() {
        val packet = MeshPacket(
            type = PacketType.MESSAGE,
            ttl = 0,
            senderId = MeshConstants.generateId()
        )
        assertThat(packet.forRelay()).isNull()
    }

    @Test
    fun `dedup key is unique per sender and packet`() {
        val sender = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val p1 = MeshPacket(type = PacketType.MESSAGE, senderId = sender, packetId = byteArrayOf(1,0,0,0,0,0,0,0))
        val p2 = MeshPacket(type = PacketType.MESSAGE, senderId = sender, packetId = byteArrayOf(2,0,0,0,0,0,0,0))
        assertThat(p1.dedupKey).isNotEqualTo(p2.dedupKey)
    }

    @Test
    fun `SOS packet has emergency priority`() {
        val packet = MeshPacket(
            type = PacketType.SOS,
            priority = MeshConstants.PRIORITY_EMERGENCY,
            senderId = MeshConstants.generateId()
        )
        assertThat(packet.priority).isEqualTo(0)
    }
}
