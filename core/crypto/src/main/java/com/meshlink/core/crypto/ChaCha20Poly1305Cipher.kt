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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ChaCha20-Poly1305 AEAD cipher for encrypting/decrypting mesh payloads.
 *
 * ## Why ChaCha20-Poly1305?
 * - Constant-time on all hardware (no AES-NI dependency)
 * - Faster than AES-GCM on devices without hardware AES
 * - Used by Noise Protocol Framework, WireGuard, TLS 1.3
 * - 256-bit key, 96-bit nonce, 128-bit authentication tag
 *
 * ## Security Properties
 * - AEAD: ciphertext integrity + authenticity guaranteed
 * - Nonce MUST be unique per key â€” managed by [NoiseCipherState]
 * - Tag verification is constant-time (JCA implementation)
 *
 * ## Wire Format
 * Encrypted payload: [nonce (12B)] [ciphertext] [tag (16B)]
 * The nonce is prepended so the receiver can decrypt.
 */
class ChaCha20Poly1305Cipher {

    private val random = SecureRandom()

    /**
     * Encrypt plaintext with ChaCha20-Poly1305.
     *
     * @param key 32-byte symmetric key
     * @param nonce 12-byte nonce (or null to auto-generate)
     * @param plaintext Data to encrypt
     * @param aad Additional Authenticated Data (optional, authenticated but not encrypted)
     * @return [nonce (12B)] + [ciphertext + tag (16B)]
     */
    fun encrypt(
        key: ByteArray,
        nonce: ByteArray? = null,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): MeshResult<ByteArray> {
        return try {
            require(key.size == 32) { "Key must be 32 bytes, got ${key.size}" }

            val actualNonce = nonce ?: ByteArray(MeshConstants.CHACHA20_NONCE_SIZE).also {
                random.nextBytes(it)
            }
            require(actualNonce.size == MeshConstants.CHACHA20_NONCE_SIZE) {
                "Nonce must be ${MeshConstants.CHACHA20_NONCE_SIZE} bytes"
            }

            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val keySpec = SecretKeySpec(key, "ChaCha20")
            val paramSpec = GCMParameterSpec(128, actualNonce)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
            if (aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            val ciphertext = cipher.doFinal(plaintext)

            // Output format: [nonce][ciphertext+tag]
            val output = ByteArray(actualNonce.size + ciphertext.size)
            System.arraycopy(actualNonce, 0, output, 0, actualNonce.size)
            System.arraycopy(ciphertext, 0, output, actualNonce.size, ciphertext.size)

            MeshResult.Success(output)
        } catch (e: Exception) {
            Timber.e(e, "ChaCha20-Poly1305 encryption failed")
            MeshResult.Error("Encryption failed", e)
        }
    }

    /**
     * Decrypt ChaCha20-Poly1305 ciphertext.
     *
     * @param key 32-byte symmetric key
     * @param ciphertextWithNonce [nonce (12B)] + [ciphertext + tag (16B)]
     * @param aad Additional Authenticated Data (must match encryption AAD)
     * @return Decrypted plaintext, or Error if authentication fails
     */
    fun decrypt(
        key: ByteArray,
        ciphertextWithNonce: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): MeshResult<ByteArray> {
        return try {
            require(key.size == 32) { "Key must be 32 bytes" }
            val nonceSize = MeshConstants.CHACHA20_NONCE_SIZE
            require(ciphertextWithNonce.size > nonceSize + MeshConstants.CHACHA20_TAG_SIZE) {
                "Ciphertext too short to contain nonce and tag"
            }

            val nonce = ciphertextWithNonce.copyOfRange(0, nonceSize)
            val ciphertext = ciphertextWithNonce.copyOfRange(nonceSize, ciphertextWithNonce.size)

            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val keySpec = SecretKeySpec(key, "ChaCha20")
            val paramSpec = GCMParameterSpec(128, nonce)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
            if (aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            val plaintext = cipher.doFinal(ciphertext)

            MeshResult.Success(plaintext)
        } catch (e: javax.crypto.AEADBadTagException) {
            Timber.w("ChaCha20-Poly1305 authentication failed â€” tampered or wrong key")
            MeshResult.Error("Authentication failed: message tampered or wrong key")
        } catch (e: Exception) {
            Timber.e(e, "ChaCha20-Poly1305 decryption failed")
            MeshResult.Error("Decryption failed", e)
        }
    }

    /**
     * Encrypt with a specific nonce value (for Noise protocol nonce counter).
     * Nonce is constructed as 4 bytes zero + 8 bytes counter (big-endian).
     */
    fun encryptWithCounter(
        key: ByteArray,
        counter: Long,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): MeshResult<ByteArray> {
        val nonce = counterToNonce(counter)
        return encrypt(key, nonce, plaintext, aad)
    }

    /**
     * Decrypt with a specific nonce counter value.
     * The nonce is NOT prepended â€” caller provides the counter directly.
     */
    fun decryptWithCounter(
        key: ByteArray,
        counter: Long,
        ciphertext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): MeshResult<ByteArray> {
        return try {
            val nonce = counterToNonce(counter)
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val keySpec = SecretKeySpec(key, "ChaCha20")
            val paramSpec = GCMParameterSpec(128, nonce)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
            if (aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            MeshResult.Success(cipher.doFinal(ciphertext))
        } catch (e: javax.crypto.AEADBadTagException) {
            MeshResult.Error("Authentication failed")
        } catch (e: Exception) {
            MeshResult.Error("Decryption failed", e)
        }
    }

    /**
     * Convert a 64-bit counter to a 12-byte Noise-compatible nonce.
     * Format: [0x00 0x00 0x00 0x00] [counter as 8-byte big-endian]
     */
    private fun counterToNonce(counter: Long): ByteArray {
        val nonce = ByteArray(MeshConstants.CHACHA20_NONCE_SIZE)
        // Last 8 bytes are the counter in big-endian
        for (i in 0 until 8) {
            nonce[11 - i] = (counter shr (i * 8)).toByte()
        }
        return nonce
    }
}
