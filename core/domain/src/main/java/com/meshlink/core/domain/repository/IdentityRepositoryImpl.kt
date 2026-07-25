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

import com.meshlink.core.common.MeshResult
import com.meshlink.core.crypto.MeshKeyPairGenerator
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the local device's identity key pair.
 *
 * Generates the key pair lazily on first access and caches it.
 * The fingerprint is a SHA-256 hash of the raw public key,
 * formatted as uppercase hex in groups of 4.
 *
 * ## API Level Compatibility
 * - API 33+: Uses Ed25519 via JCA KeyPairGenerator
 * - API 29-32: Falls back to X25519 key pair for identity
 * - Fallback: Uses SecureRandom-generated 32-byte identity
 */
@Singleton
class IdentityRepositoryImpl @Inject constructor(
    private val keyPairGenerator: MeshKeyPairGenerator
) : IdentityRepository {

    /** Lazily generated public key bytes for identity. */
    private val publicKeyBytes: ByteArray by lazy {
        // Try Ed25519 first (API 33+)
        val ed25519Result = keyPairGenerator.generateEd25519KeyPair()
        if (ed25519Result is MeshResult.Success) {
            Log.d(TAG, "Identity: using Ed25519 key pair")
            return@lazy ed25519Result.data.publicKey
        }

        // Fall back to X25519 (API 29+)
        Log.w(TAG, "Ed25519 not available, falling back to X25519 for identity")
        val x25519Result = keyPairGenerator.generateX25519KeyPair()
        if (x25519Result is MeshResult.Success) {
            Log.d(TAG, "Identity: using X25519 key pair")
            return@lazy x25519Result.data.publicKey
        }

        // Last resort: generate random 32-byte identity
        Log.w(TAG, "Key generation failed, using SecureRandom identity")
        ByteArray(32).also { SecureRandom().nextBytes(it) }
    }

    /**
     * Returns the SHA-256 fingerprint of the public key,
     * formatted as uppercase hex in groups of 4 (e.g., "A1B2 C3D4 E5F6 7890").
     */
    override fun getFingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBytes)
        // Take first 16 bytes (32 hex chars) for a readable fingerprint
        val hex = hash.take(16).joinToString("") { "%02X".format(it) }
        return hex.chunked(4).joinToString(" ")
    }

    /**
     * Returns the raw public key as an uppercase hex string.
     */
    override fun getPublicKeyHex(): String {
        return publicKeyBytes.joinToString("") { "%02X".format(it) }
    }

    companion object {
        private const val TAG = "IdentityRepository"
    }
}
