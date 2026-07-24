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

/**
 * MeshLink wire protocol packet.
 *
 * ## Binary Format (big-endian)
 * ```
 * â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
 * â”‚ Version (1B) â”‚ Type (1B) â”‚ Priority (1B) â”‚ TTL (1B)         â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Flags (1B) â”‚ Fragment# (1B) â”‚ TotalFragments (1B) â”‚ Pad (1B)â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Packet ID (8B)                                               â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Sender ID (8B)                                               â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Recipient ID (8B) â€” 0x00 for broadcast                      â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Timestamp (8B) â€” milliseconds since epoch                   â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Payload Length (2B)                                          â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Payload (variable, LZ4 compressed)                          â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ Signature (64B) â€” Ed25519 over [all except TTL and sig]     â”‚
 * â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
 * Header: 40 bytes fixed + 2 byte length + variable payload + 64 byte sig
 * ```
 *
 * ## Design Decisions
 * - TTL excluded from signature so relays can decrement without invalidation
 * - Priority field enables emergency packet preemption
 * - Fragment fields support in-header fragmentation (no separate fragment type needed)
 * - Padding byte reserved for future PKCS7-style padding
 */
data class MeshPacket(
    val version: Byte = MeshConstants.PROTOCOL_VERSION,
    val type: PacketType,
    val priority: Int = MeshConstants.PRIORITY_MESSAGE,
    val ttl: Int = MeshConstants.DEFAULT_TTL,
    val flags: PacketFlags = PacketFlags(),
    val fragmentIndex: Int = 0,
    val totalFragments: Int = 1,
    val packetId: ByteArray = MeshConstants.generateId(),
    val senderId: ByteArray,
    val recipientId: ByteArray = BROADCAST_ID,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: ByteArray = ByteArray(0),
    val signature: ByteArray? = null
) {
    /** Whether this is a broadcast packet (recipientId = all zeros). */
    val isBroadcast: Boolean
        get() = recipientId.contentEquals(BROADCAST_ID)

    /** Whether this is a fragment of a larger message. */
    val isFragment: Boolean
        get() = totalFragments > 1

    /** Whether this is the last fragment. */
    val isLastFragment: Boolean
        get() = fragmentIndex == totalFragments - 1

    /** Unique dedup key: senderId + packetId. */
    val dedupKey: String
        get() {
            val senderHex = senderId.joinToString("") { "%02x".format(it) }
            val packetHex = packetId.joinToString("") { "%02x".format(it) }
            return "$senderHex:$packetHex"
        }

    /**
     * Create a relay copy: decrement TTL, keep everything else.
     */
    fun forRelay(): MeshPacket? {
        if (ttl <= 0) return null
        return copy(ttl = ttl - 1)
    }

    /**
     * Get the bytes to sign (everything except TTL byte and signature).
     * TTL is excluded so relays can modify it without breaking verification.
     */
    fun signingPayload(): ByteArray {
        val buffer = java.nio.ByteBuffer.allocate(39 + payload.size)
        buffer.put(version)
        buffer.put(type.code.toByte())
        buffer.put(priority.toByte())
        // TTL intentionally excluded
        buffer.put(flags.toByte())
        buffer.put(fragmentIndex.toByte())
        buffer.put(totalFragments.toByte())
        buffer.put(0.toByte()) // padding
        buffer.put(packetId)
        buffer.put(senderId)
        buffer.put(recipientId)
        buffer.putLong(timestamp)
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshPacket) return false
        return packetId.contentEquals(other.packetId) && senderId.contentEquals(other.senderId)
    }

    override fun hashCode(): Int {
        var result = packetId.contentHashCode()
        result = 31 * result + senderId.contentHashCode()
        return result
    }

    companion object {
        val BROADCAST_ID = ByteArray(MeshConstants.PEER_ID_SIZE)  // All zeros
        const val HEADER_SIZE = 42  // 40 + 2 for payload length
        const val SIGNATURE_SIZE = MeshConstants.ED25519_SIGNATURE_SIZE
    }
}

/**
 * Packet type codes for the MeshLink wire protocol.
 */
enum class PacketType(val code: Int) {
    /** Peer presence announcement with capabilities. */
    ANNOUNCE(0x01),
    /** Peer departure notification. */
    LEAVE(0x02),
    /** Public broadcast message (cleartext). */
    MESSAGE(0x10),
    /** Noise XX handshake message. */
    NOISE_HANDSHAKE(0x20),
    /** Noise-encrypted private payload. */
    NOISE_ENCRYPTED(0x21),
    /** Delivery acknowledgment. */
    ACK(0x30),
    /** Store-and-forward courier envelope. */
    COURIER_ENVELOPE(0x40),
    /** History sync request. */
    SYNC_REQUEST(0x50),
    /** File transfer chunk. */
    FILE_CHUNK(0x60),
    /** Emergency SOS broadcast (highest priority). */
    SOS(0x70),
    /** Mesh channel message. */
    CHANNEL_MESSAGE(0x80);

    companion object {
        private val codeMap = entries.associateBy { it.code }
        fun fromCode(code: Int): PacketType? = codeMap[code]
    }
}

/**
 * Bitfield flags for packet metadata.
 */
data class PacketFlags(
    val compressed: Boolean = false,
    val encrypted: Boolean = false,
    val requiresAck: Boolean = false,
    val isRelay: Boolean = false,
    val sourceRouted: Boolean = false
) {
    fun toByte(): Byte {
        var flags = 0
        if (compressed) flags = flags or 0x01
        if (encrypted) flags = flags or 0x02
        if (requiresAck) flags = flags or 0x04
        if (isRelay) flags = flags or 0x08
        if (sourceRouted) flags = flags or 0x10
        return flags.toByte()
    }

    companion object {
        fun fromByte(b: Byte): PacketFlags {
            val i = b.toInt() and 0xFF
            return PacketFlags(
                compressed = (i and 0x01) != 0,
                encrypted = (i and 0x02) != 0,
                requiresAck = (i and 0x04) != 0,
                isRelay = (i and 0x08) != 0,
                sourceRouted = (i and 0x10) != 0
            )
        }
    }
}
