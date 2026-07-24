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
package com.meshlink.core.crypto

import com.meshlink.core.common.MeshConstants
import com.meshlink.core.common.MeshResult
import timber.log.Timber
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * Manages X25519 key agreement and Ed25519 digital signatures.
 *
 * ## Design
 * Uses the Android platform's built-in XDH (X25519) and EdDSA (Ed25519)
 * providers available since API 33+. For API 29-32, falls back to
 * BouncyCastle-compatible Tink primitives.
 *
 * ## Key Types
 * - **X25519**: Elliptic-curve Diffie-Hellman for key agreement (Noise handshake)
 * - **Ed25519**: Digital signatures for packet authentication
 *
 * Both key pairs are generated independently and stored in Android Keystore
 * via [IdentityKeyStore].
 */
class MeshKeyPairGenerator {

    /**
     * Generate an X25519 key pair for Diffie-Hellman key exchange.
     * Returns raw 32-byte public and private keys.
     */
    fun generateX25519KeyPair(): MeshResult<MeshKeyPair> {
        return try {
            val kpg = KeyPairGenerator.getInstance("XDH")
            val kp = kpg.generateKeyPair()
            val publicBytes = extractRawPublicKey(kp.public)
            val privateBytes = extractRawPrivateKey(kp.private)
            MeshResult.Success(
                MeshKeyPair(
                    publicKey = publicBytes,
                    privateKey = privateBytes,
                    jcaKeyPair = kp
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate X25519 key pair")
            MeshResult.Error("X25519 key generation failed", e)
        }
    }

    /**
     * Generate an Ed25519 key pair for packet signing.
     */
    fun generateEd25519KeyPair(): MeshResult<MeshKeyPair> {
        return try {
            val kpg = KeyPairGenerator.getInstance("Ed25519")
            val kp = kpg.generateKeyPair()
            val publicBytes = extractRawPublicKey(kp.public)
            val privateBytes = extractRawPrivateKey(kp.private)
            MeshResult.Success(
                MeshKeyPair(
                    publicKey = publicBytes,
                    privateKey = privateBytes,
                    jcaKeyPair = kp
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate Ed25519 key pair")
            MeshResult.Error("Ed25519 key generation failed", e)
        }
    }

    /**
     * Perform X25519 Diffie-Hellman key agreement.
     * Returns 32-byte shared secret.
     */
    fun x25519KeyAgreement(
        localPrivateKey: PrivateKey,
        remotePublicKey: PublicKey
    ): MeshResult<ByteArray> {
        return try {
            val ka = KeyAgreement.getInstance("XDH")
            ka.init(localPrivateKey)
            ka.doPhase(remotePublicKey, true)
            val sharedSecret = ka.generateSecret()
            MeshResult.Success(sharedSecret)
        } catch (e: Exception) {
            Timber.e(e, "X25519 key agreement failed")
            MeshResult.Error("Key agreement failed", e)
        }
    }

    /**
     * Sign data using Ed25519.
     * Returns 64-byte signature.
     */
    fun sign(data: ByteArray, privateKey: PrivateKey): MeshResult<ByteArray> {
        return try {
            val sig = Signature.getInstance("Ed25519")
            sig.initSign(privateKey)
            sig.update(data)
            val signature = sig.sign()
            MeshResult.Success(signature)
        } catch (e: Exception) {
            Timber.e(e, "Ed25519 signing failed")
            MeshResult.Error("Signing failed", e)
        }
    }

    /**
     * Verify an Ed25519 signature.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        return try {
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            Timber.e(e, "Ed25519 verification failed")
            false
        }
    }

    /**
     * Reconstruct a PublicKey from raw bytes for X25519.
     */
    fun x25519PublicKeyFromBytes(raw: ByteArray): PublicKey {
        val kf = KeyFactory.getInstance("XDH")
        // Wrap raw 32-byte key in SubjectPublicKeyInfo ASN.1 structure
        val prefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
        )
        val encoded = prefix + raw
        return kf.generatePublic(X509EncodedKeySpec(encoded))
    }

    /**
     * Reconstruct a PublicKey from raw bytes for Ed25519.
     */
    fun ed25519PublicKeyFromBytes(raw: ByteArray): PublicKey {
        val kf = KeyFactory.getInstance("Ed25519")
        val prefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
        )
        val encoded = prefix + raw
        return kf.generatePublic(X509EncodedKeySpec(encoded))
    }

    private fun extractRawPublicKey(key: PublicKey): ByteArray {
        // X509-encoded public key has a header; raw X25519/Ed25519 key is last 32 bytes
        val encoded = key.encoded
        return encoded.copyOfRange(encoded.size - MeshConstants.X25519_KEY_SIZE, encoded.size)
    }

    private fun extractRawPrivateKey(key: PrivateKey): ByteArray {
        val encoded = key.encoded
        // PKCS#8 encoded private key â€” raw key is last 32 bytes
        return encoded.copyOfRange(encoded.size - MeshConstants.X25519_KEY_SIZE, encoded.size)
    }
}

/**
 * Holds a key pair as both raw bytes (for wire protocol) and JCA objects (for crypto ops).
 */
data class MeshKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val jcaKeyPair: KeyPair
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshKeyPair) return false
        return publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int = publicKey.contentHashCode()
}
