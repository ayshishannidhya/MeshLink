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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HKDF (HMAC-based Key Derivation Function) using SHA-256.
 *
 * Implements RFC 5869 for deriving multiple cryptographic keys from
 * a single shared secret. Used in the Noise Protocol handshake to
 * produce symmetric encryption keys from DH outputs.
 *
 * ## Usage in MeshLink
 * - Derive send/receive cipher keys from Noise handshake
 * - Derive encryption keys for store-and-forward envelopes
 * - Key rotation: derive new keys from existing key material
 *
 * ## Security
 * - Input key material should have sufficient entropy (â‰¥256 bits)
 * - Salt improves security but can be empty per RFC 5869
 * - Info string provides domain separation between derived keys
 */
class HkdfSha256 {

    /**
     * Full HKDF: Extract + Expand.
     *
     * @param ikm Input keying material (e.g., DH shared secret)
     * @param salt Optional salt (defaults to zero-filled)
     * @param info Context/application-specific info string
     * @param length Desired output length in bytes
     * @return Derived key material of [length] bytes
     */
    fun deriveKey(
        ikm: ByteArray,
        salt: ByteArray = ByteArray(32),
        info: ByteArray = MeshConstants.HKDF_INFO.toByteArray(),
        length: Int = 32
    ): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }

    /**
     * HKDF-Extract: Produces a pseudorandom key (PRK) from input keying material.
     * PRK = HMAC-SHA256(salt, ikm)
     */
    fun extract(salt: ByteArray, ikm: ByteArray): ByteArray {
        return hmacSha256(salt, ikm)
    }

    /**
     * HKDF-Expand: Expands a PRK into output keying material.
     *
     * T(1) = HMAC(PRK, info || 0x01)
     * T(2) = HMAC(PRK, T(1) || info || 0x02)
     * ...
     * OKM = T(1) || T(2) || ... truncated to length
     */
    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 255 * 32) { "HKDF output length too large" }

        val hashLen = 32  // SHA-256 output
        val n = (length + hashLen - 1) / hashLen
        val output = ByteArray(n * hashLen)
        var previousT = ByteArray(0)

        for (i in 1..n) {
            val input = previousT + info + byteArrayOf(i.toByte())
            previousT = hmacSha256(prk, input)
            System.arraycopy(previousT, 0, output, (i - 1) * hashLen, hashLen)
        }

        return output.copyOfRange(0, length)
    }

    /**
     * Noise protocol HKDF: derives two 32-byte keys from chaining key + input.
     * This is the standard Noise HKDF call used after each DH operation.
     *
     * Returns Pair(new chaining key, derived key material)
     */
    fun noiseHkdf(chainingKey: ByteArray, inputKeyMaterial: ByteArray): Pair<ByteArray, ByteArray> {
        val tempKey = extract(chainingKey, inputKeyMaterial)
        val output1 = expand(tempKey, byteArrayOf(0x01), 32)
        val output2 = expand(tempKey, output1 + byteArrayOf(0x02), 32)
        return Pair(output1, output2)
    }

    /**
     * Noise protocol HKDF with three outputs (for split operation).
     * Returns Triple(new chaining key, key1, key2)
     */
    fun noiseHkdf3(chainingKey: ByteArray, inputKeyMaterial: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        val tempKey = extract(chainingKey, inputKeyMaterial)
        val output1 = expand(tempKey, byteArrayOf(0x01), 32)
        val output2 = expand(tempKey, output1 + byteArrayOf(0x02), 32)
        val output3 = expand(tempKey, output2 + byteArrayOf(0x03), 32)
        return Triple(output1, output2, output3)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
