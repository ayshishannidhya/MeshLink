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
package com.meshlink.core.common

import java.security.SecureRandom

/**
 * Central constants for the MeshLink protocol and system configuration.
 * All timing values are in milliseconds unless noted otherwise.
 */
object MeshConstants {

    // â”€â”€ Protocol â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val PROTOCOL_VERSION: Byte = 0x01
    const val MAX_TTL: Int = 7
    const val DEFAULT_TTL: Int = 7
    const val PACKET_ID_SIZE: Int = 8  // bytes
    const val PEER_ID_SIZE: Int = 8    // bytes

    // â”€â”€ BLE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val BLE_MTU_DEFAULT: Int = 512
    const val BLE_MTU_REQUEST: Int = 517
    const val BLE_FRAGMENT_PAYLOAD: Int = 469  // MTU minus fragment header overhead
    const val BLE_MAX_CONNECTIONS: Int = 7
    const val BLE_SERVICE_UUID: String = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    const val BLE_WRITE_CHAR_UUID: String = "a1b2c3d4-e5f6-7890-abcd-ef1234567891"
    const val BLE_READ_CHAR_UUID: String = "a1b2c3d4-e5f6-7890-abcd-ef1234567892"
    const val BLE_NOTIFY_CHAR_UUID: String = "a1b2c3d4-e5f6-7890-abcd-ef1234567893"

    // â”€â”€ Scanning & Advertising â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val SCAN_DURATION_NORMAL_MS: Long = 4_000
    const val SCAN_PAUSE_NORMAL_MS: Long = 8_000
    const val SCAN_DURATION_LOW_POWER_MS: Long = 2_000
    const val SCAN_PAUSE_LOW_POWER_MS: Long = 15_000
    const val ANNOUNCE_INTERVAL_ISOLATED_MS: Long = 4_000
    const val ANNOUNCE_INTERVAL_CONNECTED_MS: Long = 20_000

    // â”€â”€ Mesh Routing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val DEDUP_CACHE_SIZE: Int = 2_000
    const val DEDUP_EXPIRY_MS: Long = 300_000  // 5 minutes
    const val NEIGHBOR_FRESHNESS_MS: Long = 60_000  // 60 seconds
    const val RELAY_JITTER_MIN_MS: Long = 20
    const val RELAY_JITTER_MAX_MS: Long = 250
    const val DENSE_THRESHOLD: Int = 6  // links before considered dense

    // â”€â”€ Store and Forward â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val SAF_DEFAULT_TTL_MS: Long = 86_400_000  // 24 hours
    const val SAF_MAX_QUEUE_SIZE: Int = 1_000
    const val SAF_RETRY_BASE_MS: Long = 5_000
    const val SAF_RETRY_MAX_MS: Long = 300_000  // 5 minutes
    const val SAF_MAX_RETRIES: Int = 50

    // â”€â”€ Fragment Assembly â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val FRAGMENT_TIMEOUT_MS: Long = 30_000  // 30 seconds
    const val MAX_CONCURRENT_ASSEMBLIES: Int = 128
    const val MAX_REASSEMBLY_SIZE: Int = 1_048_576  // 1 MiB

    // â”€â”€ Reputation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val REPUTATION_INITIAL: Float = 0.5f
    const val REPUTATION_RELAY_REWARD: Float = 0.01f
    const val REPUTATION_RELAY_PENALTY: Float = 0.05f
    const val REPUTATION_MIN: Float = 0.0f
    const val REPUTATION_MAX: Float = 1.0f

    // â”€â”€ Rate Limiting â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val RATE_LIMIT_WINDOW_MS: Long = 60_000  // 1 minute
    const val RATE_LIMIT_MAX_PACKETS: Int = 120

    // â”€â”€ Priority Classes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val PRIORITY_EMERGENCY: Int = 0
    const val PRIORITY_COORDINATOR: Int = 1
    const val PRIORITY_MESSAGE: Int = 2
    const val PRIORITY_MEDIA: Int = 3

    // â”€â”€ Crypto â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val X25519_KEY_SIZE: Int = 32
    const val ED25519_SIGNATURE_SIZE: Int = 64
    const val CHACHA20_NONCE_SIZE: Int = 12
    const val CHACHA20_TAG_SIZE: Int = 16
    const val HKDF_INFO: String = "MeshLink-v1"
    const val REPLAY_WINDOW_SIZE: Int = 2048

    // â”€â”€ Ephemeral ID Rotation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    const val EPHEMERAL_ID_ROTATION_MS: Long = 3_600_000  // 1 hour

    /** Thread-safe SecureRandom for generating IDs and nonces. */
    val secureRandom: SecureRandom = SecureRandom()

    /** Generate a random 8-byte peer/packet ID. */
    fun generateId(): ByteArray {
        val id = ByteArray(PACKET_ID_SIZE)
        secureRandom.nextBytes(id)
        return id
    }
}
